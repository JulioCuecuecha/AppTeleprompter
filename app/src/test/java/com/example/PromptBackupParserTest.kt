package com.example

import com.example.data.sync.PromptBackupParser
import com.example.ui.utils.ScriptTagHighlighter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBackupParserTest {

    @Test
    fun parsesNestedArrayOfArraysTeleprompterProBackupAndSkipsDeleted() {
        val sampleBackup = """
            [
              [
                {
                  "title": "Noche de Escenario",
                  "plainText": "[INTRO]\nPrimera estrofa\n(Coro)\nEstribillo fuerte",
                  "scrollSpeed": 12.5,
                  "fontSize": 52,
                  "isDeleted": false
                },
                {
                  "title": "Borrador Eliminado",
                  "plainText": "No debe importarse",
                  "scrollSpeed": 10.0,
                  "fontSize": 40,
                  "isDeleted": true
                }
              ],
              [
                {
                  "title": "Balada Final",
                  "plainText": "(2da)\nCierre acústico\n[MÚSICA]",
                  "scrollSpeed": 4.5,
                  "fontSize": 60,
                  "isDeleted": 0
                }
              ]
            ]
        """.trimIndent()

        val result = PromptBackupParser.parse(sampleBackup)

        assertEquals(2, result.scripts.size)
        assertEquals(1, result.skippedDeletedCount)
        assertEquals("Noche de Escenario", result.scripts[0].title)
        assertEquals(12.5f, result.scripts[0].scrollSpeed, 0.01f)
        assertEquals(52f, result.scripts[0].fontSizeSp, 0.01f)
        assertEquals("Balada Final", result.scripts[1].title)
        assertEquals(4.5f, result.scripts[1].scrollSpeed, 0.01f)
        assertEquals(60f, result.scripts[1].fontSizeSp, 0.01f)
    }

    @Test
    fun detectsSectionCueOffsetsForPedalNavigation() {
        val lyrics = """
            [INTRO]
            Línea uno
            Línea dos
            (Coro)
            Canta fuerte
            [MÚSICA]
            Solo de guitarra
        """.trimIndent()

        val offsets = ScriptTagHighlighter.findSectionCharacterOffsets(lyrics)
        assertEquals(3, offsets.size)
        assertTrue(offsets[0] == 0)
        assertTrue(offsets[1] > offsets[0])
        assertTrue(offsets[2] > offsets[1])
    }
}
