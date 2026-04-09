package com.theveloper.pixelplay.data.ai

import com.theveloper.pixelplay.data.ai.provider.AiClientFactory
import com.theveloper.pixelplay.data.ai.provider.AiProvider
import com.theveloper.pixelplay.data.database.AiCacheDao
import com.theveloper.pixelplay.data.database.AiCacheEntity
import com.theveloper.pixelplay.data.preferences.AiPreferencesRepository
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiOrchestrator @Inject constructor(
    private val preferencesRepo: AiPreferencesRepository,
    private val clientFactory: AiClientFactory,
    private val cacheDao: AiCacheDao
) {
    private fun String.sha256(): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(this.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private suspend fun getSystemPrompt(provider: AiProvider): String {
        val prompt = when (provider) {
            AiProvider.GEMINI -> preferencesRepo.geminiSystemPrompt.first()
            AiProvider.DEEPSEEK -> preferencesRepo.deepseekSystemPrompt.first()
            AiProvider.GROQ -> preferencesRepo.groqSystemPrompt.first()
            AiProvider.MISTRAL -> preferencesRepo.mistralSystemPrompt.first()
        }
        return prompt.ifBlank { AiPreferencesRepository.DEFAULT_SYSTEM_PROMPT }
    }

    private suspend fun getApiKey(provider: AiProvider): String {
        return when (provider) {
            AiProvider.GEMINI -> preferencesRepo.geminiApiKey.first()
            AiProvider.DEEPSEEK -> preferencesRepo.deepseekApiKey.first()
            AiProvider.GROQ -> preferencesRepo.groqApiKey.first()
            AiProvider.MISTRAL -> preferencesRepo.mistralApiKey.first()
        }
    }

    private suspend fun getModel(provider: AiProvider): String {
        return when (provider) {
            AiProvider.GEMINI -> preferencesRepo.geminiModel.first()
            AiProvider.DEEPSEEK -> preferencesRepo.deepseekModel.first()
            AiProvider.GROQ -> preferencesRepo.groqModel.first()
            AiProvider.MISTRAL -> preferencesRepo.mistralModel.first()
        }
    }

    suspend fun generateContent(prompt: String): String {
        // Determine chain based on user preference
        val userProviderStr = preferencesRepo.aiProvider.first()
        val userProvider = AiProvider.fromString(userProviderStr)

        // Check cache using the primary provider's system prompt combined with the prompt
        val primarySystemPrompt = getSystemPrompt(userProvider)
        val combinedForHash = primarySystemPrompt + prompt
        val hash = combinedForHash.sha256()

        cacheDao.getCache(hash)?.responseJson?.let { return it }

        val providersToTry = mutableListOf<AiProvider>()
        providersToTry.add(userProvider)
        
        // Setup failover list prioritizing fast/free models
        if (userProvider != AiProvider.GROQ) providersToTry.add(AiProvider.GROQ)
        if (userProvider != AiProvider.MISTRAL) providersToTry.add(AiProvider.MISTRAL)
        if (userProvider != AiProvider.GEMINI) providersToTry.add(AiProvider.GEMINI)
        if (userProvider != AiProvider.DEEPSEEK) providersToTry.add(AiProvider.DEEPSEEK)
        
        var lastException: Exception? = null
        
        for (provider in providersToTry) {
            try {
                val apiKey = getApiKey(provider)
                if (apiKey.isBlank()) continue
                
                val model = getModel(provider)
                val systemPrompt = getSystemPrompt(provider)
                
                val client = clientFactory.createClient(provider, apiKey)
                val response = client.generateContent(
                    model.ifBlank { client.getDefaultModel() }, 
                    systemPrompt,
                    prompt
                )
                
                cacheDao.insert(AiCacheEntity(promptHash = hash, responseJson = response, timestamp = System.currentTimeMillis()))
                return response
            } catch (e: Exception) {
                lastException = e
            }
        }
        
        throw Exception("All AI providers failed. Check your API keys. Last error: ${lastException?.message}", lastException)
    }
}
