package com.theveloper.pixelplay.data.ai


import javax.inject.Inject
import javax.inject.Singleton

enum class AiSystemPromptType {
    PLAYLIST,
    METADATA,
    TAGGING,
    MOOD_ANALYSIS,
    PERSONA,
    GENERAL
}

@Singleton
class AiSystemPromptEngine @Inject constructor() {

    // AI Optimization: Centralized universal constraints to prevent common LLM "chatter" behaviors.
    private val UNIVERSAL_CONSTRAINTS = """
        <universal_constraints>
        - NEVER output markdown code blocks (e.g. no ```json or ```csv).
        - NEVER include introductory conversational text (e.g. "Sure!", "Here is...").
        - NEVER include explanatory text or closing remarks.
        - NEVER include formatting characters like '*' or '#' unless required by the data format.
        - STRICTLY return the raw data string ONLY.
        - Failure to follow these formatting rules will break the application parser.
        </universal_constraints>
    """.trimIndent()

    fun buildPrompt(basePersona: String, type: AiSystemPromptType, context: String = ""): String {
        val requirementLayer = when (type) {
            AiSystemPromptType.PLAYLIST -> """
                <instructions>
                Act as a precision background process for a music application. Your task is to curate a list of song IDs.
                Analyze the user's request and cross-reference it with their provided listening context.
                Choose tracks that create a cohesive flow and align with the expressed vibe.
                </instructions>
                
                <strategy>
                - DISCOVERY: If the user asks for new/unheard/discovery music, you MUST select IDs from the [DISCOVERY_POOL].
                - FAVORITES: If the user asks for favorites/vibe-matching, prioritize IDs with high [p] counts or [f:1] flags from [LISTENED_TRACKS].
                - BALANCE: Mix both pools if the request is generic, favoring the user's top genres/artists.
                </strategy>
                
                <output_format>
                Return a RAW JSON ARRAY of song IDs.
                Example: ["id_123", "id_456", "id_789"]
                </output_format>
            """.trimIndent()

            AiSystemPromptType.METADATA -> """
                <instructions>
                You are a technical metadata specialist. Provide the most accurate, industry-standard values for the given song.
                Avoid generic genres; be specific (e.g. "Synthwave" instead of "Electronic").
                </instructions>
                
                <output_format>
                Return a RAW JSON OBJECT.
                Format: {"title": "Full Song Title", "artist": "Primary Artist", "album": "Official Album Name", "genre": "Specific Genre"}
                </output_format>
            """.trimIndent()

            AiSystemPromptType.TAGGING -> """
                <instructions>
                Generate 6 to 10 highly descriptive, atmospheric, and technical tags for the audio.
                Tags should be lowercase and hyphenated (e.g. "ethereal-vocals", "lo-fi-beats").
                </instructions>
                
                <output_format>
                Return a RAW COMMA-SEPARATED list.
                Example: cinematic, orchestral, tension-builder, dark-ambient, hybrid-score
                </output_format>
            """.trimIndent()

            AiSystemPromptType.MOOD_ANALYSIS -> """
                <instructions>
                Perform a deep signal analysis of the song description/metadata to derive psychological and energetic metrics.
                Metrics must be floats between 0.0 and 1.0. 
                Possible Moods: Joyful, Aggressive, Calm, Melancholic, Radiant, Intense, Somber.
                </instructions>
                
                <output_format>
                Tag | Value mapping.
                Format: [Mood] | Energy:X.X | Valence:X.X | Danceability:X.X | Acousticness:X.X
                Example: Intense | Energy:0.9 | Valence:0.1 | Danceability:0.4 | Acousticness:0.0
                </output_format>
            """.trimIndent()

            AiSystemPromptType.PERSONA -> """
                <instructions>
                Respond as a poetic sonic oracle. Your tone is sophisticated, slightly enigmatic, yet deeply empathetic to the user's taste.
                Reference the user's specific stats (e.g. "Your dedication to [Artist] is noted...") to personalize the interaction.
                IMPORTANT: Even in persona mode, be concise.
                </instructions>
            """.trimIndent()

            AiSystemPromptType.GENERAL -> """
                <instructions>
                You are the core logic engine of PixelPlayer. Help the user with music-related queries using their library context.
                </instructions>
            """.trimIndent()
        }

        val contextLayer = if (context.isNotBlank()) {
            """
            <user_listening_context>
            $context
            
            <stats_interpretation_protocol>
            - [LISTENED_TRACKS]: Tracks the user has played.
            - format: id | p(play_count) | d(total_minutes) | f(is_favorite:1/0) | meta(title-artist)
            - [DISCOVERY_POOL]: Tracks in the user's library with 0 plays. Use these for discovery requests.
            </stats_interpretation_protocol>
            </user_listening_context>
            """.trimIndent()
        } else ""

        return """
            <system_persona>
            $basePersona
            </system_persona>

            $UNIVERSAL_CONSTRAINTS
            
            $contextLayer
            
            <type_specific_requirements>
            $requirementLayer
            </type_specific_requirements>
            
            FINAL WARNING: RETURN DATA ONLY. NO MARKDOWN. NO CHAT.
        """.trimIndent()
    }
}
