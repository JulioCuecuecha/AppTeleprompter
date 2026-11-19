package com.example.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.CueChorusCoral
import com.example.ui.theme.CueChorusCoralBg
import com.example.ui.theme.CueMusicCyan
import com.example.ui.theme.CueMusicCyanBg
import com.example.ui.theme.CueVocalEmerald
import com.example.ui.theme.CueVocalEmeraldBg

object ScriptTagHighlighter {

    val QuickCueTags = listOf(
        "[MÚSICA]",
        "(Coro)",
        "(2da)",
        "[INTRO]",
        "[PUENTE]",
        "[SOLO]",
        "[FINAL]"
    )

    private val bracketTagRegex = Regex("""\[[^\]\n]{1,48}\]""")
    private val parenTagRegex = Regex("""\([^)\n]{1,48}\)""")
    private val chorusKeywordRegex = Regex(
        pattern = """(coro|estribillo|chorus|pre-?coro)""",
        option = RegexOption.IGNORE_CASE
    )
    private val vocalCueRegex = Regex(
        pattern = """(2da|1ra|3ra|voz|todos|coros|armon[ií]a|falsete|hablado|bajo|suave|fuerte)""",
        option = RegexOption.IGNORE_CASE
    )

    /**
     * Builds a color-highlighted AnnotatedString for stage cues:
     * - `[MÚSICA]`, `[INTRO]`, `[SOLO]`, etc. -> Broadcast Cyan highlight
     * - `(Coro)`, `(Pre-Coro)`, `(Estribillo)` -> Stage Coral highlight
     * - `(2da)`, `(1ra)`, `(Todos)`, etc. -> Vocal Emerald highlight
     * - Other `(...)` cues -> Subtle Broadcast Amber accent
     */
    fun highlight(text: String): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        return buildAnnotatedString {
            append(text)

            bracketTagRegex.findAll(text).forEach { match ->
                addStyle(
                    style = SpanStyle(
                        color = CueMusicCyan,
                        background = CueMusicCyanBg,
                        fontWeight = FontWeight.Bold
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }

            parenTagRegex.findAll(text).forEach { match ->
                val value = match.value
                val style = when {
                    chorusKeywordRegex.containsMatchIn(value) -> SpanStyle(
                        color = CueChorusCoral,
                        background = CueChorusCoralBg,
                        fontWeight = FontWeight.Bold
                    )
                    vocalCueRegex.containsMatchIn(value) -> SpanStyle(
                        color = CueVocalEmerald,
                        background = CueVocalEmeraldBg,
                        fontWeight = FontWeight.SemiBold
                    )
                    else -> SpanStyle(
                        color = BroadcastAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                addStyle(
                    style = style,
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }
    }

    /**
     * Returns character indices of lines containing section cue tags (`[...]` or `(Coro)` etc.)
     * so Bluetooth pedal PageUp / PageDown can jump cleanly between song sections.
     */
    fun findSectionCharacterOffsets(text: String): List<Int> {
        if (text.isBlank()) return emptyList()
        val offsets = mutableListOf<Int>()
        var cursor = 0
        text.split('\n').forEach { line ->
            val trimmed = line.trim()
            val isSectionLine = trimmed.isNotEmpty() && (
                bracketTagRegex.containsMatchIn(trimmed) ||
                    chorusKeywordRegex.containsMatchIn(trimmed) ||
                    (trimmed.startsWith("(") && trimmed.endsWith(")"))
                )
            if (isSectionLine) {
                offsets.add(cursor)
            }
            cursor += line.length + 1
        }
        return offsets
    }
}

class ScriptCueVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = ScriptTagHighlighter.highlight(text.text),
            offsetMapping = OffsetMapping.Identity
        )
    }
}
