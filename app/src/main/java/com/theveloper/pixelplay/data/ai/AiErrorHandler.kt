package com.theveloper.pixelplay.data.ai

import com.theveloper.pixelplay.data.ai.provider.AiProviderException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiErrorHandler @Inject constructor() {

    fun resolveErrorMessage(error: Throwable): String {
        val providerFailure = error.findProviderFailure()
        val detail = extractDetail(error)

        return when {
            providerFailure?.isApiKeyIssue() == true ||
                detail.contains("api key not valid", ignoreCase = true) ||
                detail.contains("invalid api key", ignoreCase = true) ||
                detail.contains("incorrect api key", ignoreCase = true) ||
                detail.contains("invalid key", ignoreCase = true) ||
                detail.contains("unauthorized", ignoreCase = true) ||
                detail.contains("401", ignoreCase = true) ->
                "Invalid or missing API key. Go to Settings to configure your API key."

            providerFailure?.isBillingIssue() == true ||
                detail.contains("insufficient_quota", ignoreCase = true) ||
                detail.contains("quota", ignoreCase = true) ||
                detail.contains("billing", ignoreCase = true) ||
                detail.contains("429", ignoreCase = true) ||
                detail.contains("rate limit", ignoreCase = true) ||
                detail.contains("too many requests", ignoreCase = true) ->
                "API quota exceeded or rate limited. Check your provider billing or wait before trying again."

            providerFailure?.isModelUnavailable() == true ->
                "The selected AI model is unavailable. Try selecting a different model in AI Settings."

            detail.contains("timed out", ignoreCase = true) ||
                detail.contains("timeout", ignoreCase = true) ->
                "Request timed out. The AI provider is slow or overloaded. Try again in a moment."

            detail.contains("network", ignoreCase = true) ||
                detail.contains("connect", ignoreCase = true) ||
                detail.contains("resolve host", ignoreCase = true) ||
                detail.contains("socketexception", ignoreCase = true) ||
                detail.contains("no internet", ignoreCase = true) ||
                detail.contains("offline", ignoreCase = true) ->
                "No Internet Connection. Check your WiFi or mobile data and try again."

            detail.contains("airplane", ignoreCase = true) ->
                "Airplane mode is on. Turn it off to use AI features."

            detail.contains("permission", ignoreCase = true) ||
                detail.contains("denied", ignoreCase = true) ||
                detail.contains("forbidden", ignoreCase = true) ||
                detail.contains("403", ignoreCase = true) ->
                "Permission denied by the AI provider. Check that this API key has access to the selected model."

            detail.contains("safety", ignoreCase = true) ||
                detail.contains("blocked", ignoreCase = true) ||
                detail.contains("filtered", ignoreCase = true) ->
                "Content was blocked by the AI's safety filters. Try rephrasing your request."

            detail.contains("valid playlist", ignoreCase = true) ||
                detail.contains("json array", ignoreCase = true) ||
                detail.contains("invalid response", ignoreCase = true) ->
                "The AI returned an unexpected format. Try again or switch to a more capable model."

            detail.contains("no api key", ignoreCase = true) ||
                detail.contains("not configured", ignoreCase = true) ->
                "No API key configured. Go to Settings to set up your API key."

            detail.contains("cooldown", ignoreCase = true) ->
                "AI providers are cooling down after recent errors. Wait a few minutes and try again."

            detail.contains("empty response", ignoreCase = true) ->
                "The AI returned an empty response. The model may have filtered the content. Try a different prompt."

            else -> "AI Error: ${detail.ifBlank { "An unexpected error occurred." }}"
        }
    }

    fun extractDetail(error: Throwable): String {
        return generateSequence(error) { it.cause }
            .flatMap { sequenceOf(it.message.orEmpty()) }
            .map { raw -> raw.replace(Regex("^AI\\s*Error:\\s*", RegexOption.IGNORE_CASE), "").trim() }
            .firstOrNull { it.isNotBlank() }
            ?: "Unknown error"
    }

    private fun Throwable.findProviderFailure(): AiProviderException? {
        return generateSequence(this) { it.cause }
            .filterIsInstance<AiProviderException>()
            .firstOrNull()
    }

    fun buildDetailedErrorMessage(e: Exception): String {
        val rootMessage = e.message?.takeIf { it.isNotBlank() }
        val causeMessage = e.cause?.message?.takeIf { it.isNotBlank() }
        val combined = listOfNotNull(rootMessage, causeMessage).joinToString(" → ")

        return when {
            combined.contains("timeout", ignoreCase = true) ||
                combined.contains("timed out", ignoreCase = true) ->
                "Request timed out. The AI provider may be slow or overloaded. Try again."

            combined.contains("network", ignoreCase = true) ||
                combined.contains("connect", ignoreCase = true) ||
                combined.contains("socketexception", ignoreCase = true) ||
                combined.contains("no internet", ignoreCase = true) ->
                "No Internet Connection. Check your WiFi or mobile data and try again."

            combined.contains("401", ignoreCase = true) ||
                combined.contains("unauthorized", ignoreCase = true) ->
                "Permission Denied. Your API key might be invalid or restricted."

            combined.contains("403", ignoreCase = true) ||
                combined.contains("permission", ignoreCase = true) ||
                combined.contains("forbidden", ignoreCase = true) ->
                "Permission denied by the AI provider. Check that this API key has access to the selected model."

            combined.contains("safety", ignoreCase = true) ||
                combined.contains("blocked", ignoreCase = true) ->
                "Content was blocked by safety filters. Try rephrasing your prompt."

            combined.contains("model", ignoreCase = true) &&
                (combined.contains("not found", ignoreCase = true) ||
                    combined.contains("unavailable", ignoreCase = true)) ->
                "The selected AI model is unavailable. Try selecting a different model in AI Settings."

            rootMessage != null -> "AI Error: $rootMessage"
            causeMessage != null -> "AI Error: $causeMessage"
            else -> "An unexpected error occurred. Try again."
        }
    }
}
