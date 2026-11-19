package com.example.data.sync

import com.example.data.model.ScriptEntity
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

data class ParsedPromptScript(
    val title: String,
    val content: String,
    val scrollSpeed: Float,
    val fontSizeSp: Float,
    val folderName: String
)

data class PromptBackupParseResult(
    val scripts: List<ParsedPromptScript>,
    val skippedDeletedCount: Int
)

/**
 * Gson-powered parser compatible with Teleprompter Pro (iOS/iPadOS) `.prompt` and `.json` exports.
 *
 * Supports:
 * 1. Native Teleprompter Pro array-of-arrays format:
 *    `[[{ "title": "...", "plainText": "...", "scrollSpeed": 12.0, "fontSize": 48, "isDeleted": false }, ...], ...]`
 * 2. Flat JSON array of script objects:
 *    `[{ "title": "...", "plainText": "...", ... }]`
 * 3. Wrapper objects containing `"scripts"`, `"items"`, or `"groups"` arrays.
 */
object PromptBackupParser {

    const val DEFAULT_IMPORT_FOLDER = "Teleprompter Pro"

    fun parse(rawJson: String, fallbackFolder: String = DEFAULT_IMPORT_FOLDER): PromptBackupParseResult {
        val cleaned = rawJson.trimStart('\uFEFF').trim()
        if (cleaned.isEmpty()) {
            return PromptBackupParseResult(emptyList(), 0)
        }

        val root = JsonParser.parseString(cleaned)
        val collected = mutableListOf<ParsedPromptScript>()
        var skippedDeleted = 0

        fun visitElement(element: JsonElement?, inheritedFolder: String) {
            if (element == null || element.isJsonNull) return
            when {
                element.isJsonArray -> {
                    element.asJsonArray.forEach { child ->
                        visitElement(child, inheritedFolder)
                    }
                }
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    // Check if this object is a container/group wrapper rather than a script leaf
                    val hasScriptFields = obj.has("plainText") || obj.has("text") ||
                        obj.has("content") || obj.has("body") || obj.has("title")
                    val nestedList = obj.get("scripts")
                        ?: obj.get("items")
                        ?: obj.get("groups")
                        ?: obj.get("documents")

                    if (nestedList != null && nestedList.isJsonArray) {
                        val groupName = obj.getStringOrNull("name")
                            ?: obj.getStringOrNull("folderName")
                            ?: obj.getStringOrNull("title")
                            ?: inheritedFolder
                        visitElement(nestedList, groupName)
                        if (!obj.has("plainText") && !obj.has("text") && !obj.has("content")) {
                            return
                        }
                    }

                    if (!hasScriptFields) return

                    if (obj.isDeletedFlag()) {
                        skippedDeleted++
                        return
                    }

                    val rawTitle = obj.getStringOrNull("title")
                        ?: obj.getStringOrNull("name")
                        ?: "Sin título"
                    val title = rawTitle.trim().ifEmpty { "Sin título" }

                    val rawContent = obj.getStringOrNull("plainText")
                        ?: obj.getStringOrNull("text")
                        ?: obj.getStringOrNull("content")
                        ?: obj.getStringOrNull("body")
                        ?: ""
                    val content = rawContent.replace("\r\n", "\n").replace("\r", "\n")

                    val rawSpeed = obj.getFloatOrNull("scrollSpeed")
                        ?: obj.getFloatOrNull("speed")
                        ?: ScriptEntity.DEFAULT_SCROLL_SPEED
                    val scrollSpeed = rawSpeed.coerceIn(
                        ScriptEntity.MIN_SCROLL_SPEED,
                        ScriptEntity.MAX_SCROLL_SPEED
                    )

                    val rawFontSize = obj.getFloatOrNull("fontSize")
                        ?: obj.getFloatOrNull("fontSizeSp")
                        ?: obj.getFloatOrNull("textSize")
                        ?: ScriptEntity.DEFAULT_FONT_SIZE_SP
                    val fontSizeSp = rawFontSize.coerceIn(
                        ScriptEntity.MIN_FONT_SIZE_SP,
                        ScriptEntity.MAX_FONT_SIZE_SP
                    )

                    val folder = obj.getStringOrNull("folderName")
                        ?: obj.getStringOrNull("groupName")
                        ?: obj.getStringOrNull("folder")
                        ?: obj.getStringOrNull("category")
                        ?: inheritedFolder

                    collected.add(
                        ParsedPromptScript(
                            title = title,
                            content = content,
                            scrollSpeed = scrollSpeed,
                            fontSizeSp = fontSizeSp,
                            folderName = folder.trim().ifEmpty { fallbackFolder }
                        )
                    )
                }
            }
        }

        visitElement(root, fallbackFolder)
        return PromptBackupParseResult(
            scripts = collected,
            skippedDeletedCount = skippedDeleted
        )
    }

    private fun JsonObject.isDeletedFlag(): Boolean {
        val el = get("isDeleted") ?: get("deleted") ?: return false
        if (!el.isJsonPrimitive) return false
        val primitive = el.asJsonPrimitive
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isNumber -> primitive.asInt != 0
            primitive.isString -> primitive.asString.equals("true", ignoreCase = true) ||
                primitive.asString == "1"
            else -> false
        }
    }

    private fun JsonObject.getStringOrNull(key: String): String? {
        val el = get(key) ?: return null
        if (el.isJsonNull || !el.isJsonPrimitive) return null
        return runCatching { el.asString }.getOrNull()
    }

    private fun JsonObject.getFloatOrNull(key: String): Float? {
        val el = get(key) ?: return null
        if (el.isJsonNull || !el.isJsonPrimitive) return null
        return runCatching { el.asFloat }.getOrNull()
            ?.takeIf { !it.isNaN() && !it.isInfinite() }
    }
}
