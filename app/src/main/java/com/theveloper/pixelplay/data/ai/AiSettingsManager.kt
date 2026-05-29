package com.theveloper.pixelplay.data.ai

import com.theveloper.pixelplay.data.ai.provider.AiProvider
import com.theveloper.pixelplay.data.preferences.AiPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiSettingsManager @Inject constructor(
    private val preferencesRepo: AiPreferencesRepository
) {
    val aiProvider: Flow<String> = preferencesRepo.aiProvider
    val isSafeTokenLimitEnabled: Flow<Boolean> = preferencesRepo.isSafeTokenLimitEnabled

    fun getApiKey(provider: AiProvider): Flow<String> = preferencesRepo.getApiKey(provider)
    fun getModel(provider: AiProvider): Flow<String> = preferencesRepo.getModel(provider)
    fun getSystemPrompt(provider: AiProvider): Flow<String> = preferencesRepo.getSystemPrompt(provider)

    val currentProvider: Flow<AiProvider> = preferencesRepo.aiProvider.map { AiProvider.fromString(it) }

    suspend fun set(block: suspend AiPreferencesRepository.() -> Unit) {
        preferencesRepo.block()
    }

    suspend fun setAiProvider(provider: String) = preferencesRepo.setAiProvider(provider)
    suspend fun setApiKey(provider: AiProvider, apiKey: String) = preferencesRepo.setApiKey(provider, apiKey)
    suspend fun setModel(provider: AiProvider, model: String) = preferencesRepo.setModel(provider, model)
    suspend fun setSystemPrompt(provider: AiProvider, prompt: String) = preferencesRepo.setSystemPrompt(provider, prompt)
    suspend fun resetSystemPrompt(provider: AiProvider) = preferencesRepo.resetSystemPrompt(provider)
    suspend fun setSafeTokenLimitEnabled(enabled: Boolean) = preferencesRepo.setSafeTokenLimitEnabled(enabled)
}
