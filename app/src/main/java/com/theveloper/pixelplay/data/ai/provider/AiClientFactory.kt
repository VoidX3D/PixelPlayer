package com.theveloper.pixelplay.data.ai.provider

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiClientFactory @Inject constructor() {

    fun createClient(provider: AiProvider, apiKey: String): AiClient {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("API Key cannot be blank for ${provider.displayName}")
        }

        return when (provider) {
            AiProvider.GEMINI -> GeminiAiClient(apiKey)
            else -> {
                val config = AiProviderEndpoints.getConfig(provider)
                GenericOpenAiClient(
                    apiKey = apiKey,
                    baseUrl = config.baseUrl,
                    defaultModelId = config.defaultModel,
                    providerName = config.displayName
                )
            }
        }
    }
}
