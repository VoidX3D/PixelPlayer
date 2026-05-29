package com.theveloper.pixelplay.data.ai

import com.theveloper.pixelplay.data.ai.provider.AiClientFactory
import com.theveloper.pixelplay.data.ai.provider.AiProvider
import com.theveloper.pixelplay.data.ai.provider.AiProviderSupport
import com.theveloper.pixelplay.data.database.AiUsageDao
import com.theveloper.pixelplay.data.database.AiUsageEntity
import com.theveloper.pixelplay.data.preferences.AiPreferencesRepository
import com.theveloper.pixelplay.di.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiHandler @Inject constructor(
    private val preferencesRepo: AiPreferencesRepository,
    private val clientFactory: AiClientFactory,
    private val cacheManager: AiCacheManager,
    private val usageDao: AiUsageDao,
    private val promptEngine: AiSystemPromptEngine,
    private val logger: AiLogger,
    @AppScope private val appScope: CoroutineScope
) {
    private val providerCooldowns = mutableMapOf<AiProvider, Long>()
    private val COOLDOWN_DURATION_MS = 1000L * 60 * 5
    private val REQUEST_TIMEOUT_MS = 60_000L

    private suspend fun getBasePersona(provider: AiProvider): String {
        return preferencesRepo.getSystemPrompt(provider).first()
            .ifBlank { AiPreferencesRepository.DEFAULT_SYSTEM_PROMPT }
    }

    private suspend fun getApiKey(provider: AiProvider): String {
        return preferencesRepo.getApiKey(provider).first()
    }

    private suspend fun getModel(provider: AiProvider): String {
        return preferencesRepo.getModel(provider).first()
    }

    private suspend fun setModel(provider: AiProvider, model: String) {
        preferencesRepo.setModel(provider, model)
    }

    private suspend fun generateWithRecovery(
        provider: AiProvider,
        apiKey: String,
        systemPrompt: String,
        prompt: String,
        temperature: Float
    ): String {
        val client = clientFactory.createClient(provider, apiKey)
        val requestedModel = getModel(provider).ifBlank { client.getDefaultModel() }

        return try {
            withTimeout(REQUEST_TIMEOUT_MS) {
                client.generateContent(requestedModel, systemPrompt, prompt, temperature)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw AiProviderSupport.createException(
                providerName = provider.displayName,
                statusCode = null,
                transportMessage = "Request timed out after ${REQUEST_TIMEOUT_MS / 1000}s. The model may be overloaded.",
                responseBody = null,
                requestedModel = requestedModel
            )
        } catch (e: Exception) {
            val failure = AiProviderSupport.wrapThrowable(provider.displayName, e, requestedModel)
            val recoveredModel = recoverModelIfNeeded(provider, apiKey, requestedModel, client, failure)
                ?: throw failure

            withTimeout(REQUEST_TIMEOUT_MS) {
                client.generateContent(recoveredModel, systemPrompt, prompt, temperature)
            }
        }
    }

    private suspend fun recoverModelIfNeeded(
        provider: AiProvider,
        apiKey: String,
        requestedModel: String,
        client: com.theveloper.pixelplay.data.ai.provider.AiClient,
        failure: com.theveloper.pixelplay.data.ai.provider.AiProviderException
    ): String? {
        if (!failure.isModelUnavailable()) return null

        val availableModels = runCatching { client.getAvailableModels(apiKey) }.getOrDefault(emptyList())
        val recoveredModel = AiProviderSupport.selectRecoveryModel(
            currentModel = requestedModel,
            defaultModel = client.getDefaultModel(),
            availableModels = availableModels
        ) ?: return null

        setModel(provider, recoveredModel)
        return recoveredModel
    }

    suspend fun generateContent(
        prompt: String,
        type: AiSystemPromptType = AiSystemPromptType.GENERAL,
        temperature: Float = 0.7f,
        context: String = ""
    ): String {
        val resolvedTemperature = if (temperature == 0.7f) {
            when (type) {
                AiSystemPromptType.METADATA -> 0.1f
                AiSystemPromptType.MOOD_ANALYSIS -> 0.2f
                AiSystemPromptType.TAGGING -> 0.4f
                AiSystemPromptType.PLAYLIST, AiSystemPromptType.DAILY_MIX -> 0.6f
                AiSystemPromptType.PERSONA -> 0.85f
                AiSystemPromptType.GENERAL -> 0.7f
            }
        } else temperature

        val userProviderStr = preferencesRepo.aiProvider.first()
        val userProvider = AiProvider.fromString(userProviderStr)

        val basePersona = getBasePersona(userProvider)
        val combinedSystemPrompt = promptEngine.buildPrompt(basePersona, type, context)

        val hash = cacheManager.buildHash(userProvider.name, combinedSystemPrompt, prompt)
        cacheManager.get(hash)?.let { return it }

        val providersToTry = AiProviderSupport.buildProviderChain(userProvider)
        val failedProviders = mutableListOf<String>()
        val now = System.currentTimeMillis()

        for (provider in providersToTry) {
            val cooldownExpiry = providerCooldowns[provider] ?: 0L
            if (now < cooldownExpiry) {
                failedProviders.add("${provider.name}: on cooldown (${((cooldownExpiry - now) / 1000)}s remaining)")
                continue
            }

            try {
                val apiKey = getApiKey(provider)
                if (apiKey.isBlank()) {
                    failedProviders.add("${provider.name}: no API key configured")
                    continue
                }

                val providerPersona = getBasePersona(provider)
                val finalSystemPrompt = promptEngine.buildPrompt(providerPersona, type, context)

                val response = generateWithRecovery(
                    provider = provider,
                    apiKey = apiKey,
                    systemPrompt = finalSystemPrompt,
                    prompt = prompt,
                    temperature = resolvedTemperature
                )

                if (response.isBlank()) {
                    failedProviders.add("${provider.name}: returned empty response")
                    continue
                }

                val isThinkingModel = finalSystemPrompt.contains("think", true) || provider.name.contains("reasoning", true)
                val estimatedPromptTokens = (finalSystemPrompt.length + prompt.length) / 4
                val estimatedOutputTokens = response.length / 4
                val estimatedThoughtTokens = if (isThinkingModel) (estimatedOutputTokens * 1.5).toInt() else 0

                appScope.launch {
                    runCatching {
                        usageDao.insertUsage(
                            AiUsageEntity(
                                timestamp = now,
                                provider = provider.displayName,
                                model = provider.name,
                                promptType = type.name,
                                promptTokens = estimatedPromptTokens,
                                outputTokens = estimatedOutputTokens,
                                thoughtTokens = estimatedThoughtTokens
                            )
                        )
                    }.onFailure { error ->
                        logger.error("AiHandler", "Failed to persist AI usage", error)
                    }
                }

                cacheManager.put(hash, response)
                return response
            } catch (e: Exception) {
                val failure = AiProviderSupport.wrapThrowable(provider.displayName, e)
                logger.warn("AiHandler", "Provider ${provider.name} failed: ${failure.message}")
                failedProviders.add("${provider.name}: ${failure.message ?: "Unknown error"}")
                if (failure.shouldCooldown()) {
                    providerCooldowns[provider] = now + COOLDOWN_DURATION_MS
                }
            }
        }

        val errorMessage = when {
            failedProviders.all { it.contains("no API key") } ->
                "No API key configured. Go to Settings → AI Integration to set up your API key."
            failedProviders.all { it.contains("cooldown") } ->
                "All AI providers are on cooldown after recent errors. Wait a few minutes and try again."
            failedProviders.size == 1 ->
                "AI generation failed: ${failedProviders.first()}"
            else ->
                "AI generation failed after trying ${failedProviders.size} providers:\n${failedProviders.joinToString("\n• ", prefix = "• ")}"
        }

        logger.error("AiHandler", "All providers failed. Details: ${failedProviders.joinToString(" | ")}")
        throw Exception(errorMessage)
    }
}
