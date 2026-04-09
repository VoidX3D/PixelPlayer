package com.theveloper.pixelplay.data.ai

import javax.inject.Inject
import javax.inject.Singleton

enum class AiSystemPromptType {
    PLAYLIST,
    METADATA,
    GENERAL
}

@Singleton
class AiSystemPromptEngine @Inject constructor() {

    fun buildPrompt(basePersona: String, type: AiSystemPromptType): String {
        val requirementLayer = when (type) {
            AiSystemPromptType.PLAYLIST -> """
                ---
                STRICT OUTPUT RULES:
                1. Your response MUST be ONLY a raw JSON array of song IDs.
                2. NO markdown code blocks (no ```json).
                3. NO conversational text, NO explanations.
                4. Example: ["id1", "id2", "id3"]
            """.trimIndent()

            AiSystemPromptType.METADATA -> """
                ---
                STRICT OUTPUT RULES:
                1. Your response MUST be ONLY a raw JSON object matching this schema: 
                   {"title": "...", "artist": "...", "album": "...", "genre": "..."}
                2. Fill in ONLY the requested fields, use null or empty string for others.
                3. NO markdown, NO conversational text.
            """.trimIndent()

            AiSystemPromptType.GENERAL -> ""
        }

        return """
            $basePersona
            
            $requirementLayer
        """.trimIndent()
    }
}
