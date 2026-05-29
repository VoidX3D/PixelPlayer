package com.theveloper.pixelplay.data.ai.provider

data class AiModelInfo(
    val name: String,
    val displayName: String
)

object AiProviderEndpoints {
    data class ProviderConfig(
        val baseUrl: String,
        val defaultModel: String,
        val displayName: String,
        val defaultModels: List<String> = emptyList()
    )

    val configs: Map<AiProvider, ProviderConfig> = mapOf(
        AiProvider.GEMINI to ProviderConfig(
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            defaultModel = "gemini-3.1-flash-lite",
            displayName = "Google Gemini",
            defaultModels = listOf("gemini-3.1-flash-lite", "gemini-3.5-flash", "gemini-flash-latest")
        ),
        AiProvider.DEEPSEEK to ProviderConfig(
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-chat",
            displayName = "DeepSeek",
            defaultModels = listOf("deepseek-chat", "deepseek-reasoner")
        ),
        AiProvider.GROQ to ProviderConfig(
            baseUrl = "https://api.groq.com/openai/v1",
            defaultModel = "llama-3.1-8b-instant",
            displayName = "Groq",
            defaultModels = listOf("llama-3.1-8b-instant", "llama-3.3-70b-versatile", "mixtral-8x7b-32768", "gemma2-9b-it")
        ),
        AiProvider.MISTRAL to ProviderConfig(
            baseUrl = "https://api.mistral.ai/v1",
            defaultModel = "mistral-large-latest",
            displayName = "Mistral",
            defaultModels = listOf("mistral-large-latest", "mistral-small-latest", "open-mixtral-8x22b", "open-mixtral-8x7b")
        ),
        AiProvider.NVIDIA to ProviderConfig(
            baseUrl = "https://integrate.api.nvidia.com/v1",
            defaultModel = "meta/llama-3.1-8b-instruct",
            displayName = "NVIDIA NIM"
        ),
        AiProvider.KIMI to ProviderConfig(
            baseUrl = "https://api.moonshot.cn/v1",
            defaultModel = "moonshot-v1-8k",
            displayName = "Kimi (Moonshot)"
        ),
        AiProvider.GLM to ProviderConfig(
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            defaultModel = "glm-4",
            displayName = "Zhipu GLM"
        ),
        AiProvider.OPENAI to ProviderConfig(
            baseUrl = "https://api.openai.com/v1",
            defaultModel = "gpt-4o-mini",
            displayName = "OpenAI"
        ),
        AiProvider.OPENROUTER to ProviderConfig(
            baseUrl = "https://openrouter.ai/api/v1",
            defaultModel = "google/gemini-3.1-flash-lite",
            displayName = "OpenRouter"
        )
    )

    fun getConfig(provider: AiProvider): ProviderConfig = configs[provider] ?: configs[AiProvider.OPENAI]!!

    fun baseUrl(provider: AiProvider): String = getConfig(provider).baseUrl

    fun defaultModel(provider: AiProvider): String = getConfig(provider).defaultModel

    fun displayName(provider: AiProvider): String = getConfig(provider).displayName

    fun defaultModels(provider: AiProvider): List<String> = getConfig(provider).defaultModels

    fun getAvailableModelsEndpoint(provider: AiProvider): String? {
        return when (provider) {
            AiProvider.GEMINI -> null
            else -> "${baseUrl(provider).trimEnd('/')}/models"
        }
    }

    val openAiCompatibleProviders: Set<AiProvider> = setOf(
        AiProvider.DEEPSEEK, AiProvider.GROQ, AiProvider.MISTRAL,
        AiProvider.NVIDIA, AiProvider.KIMI, AiProvider.GLM,
        AiProvider.OPENAI, AiProvider.OPENROUTER
    )
}
