package com.example.data.model

import androidx.compose.ui.text.style.TextAlign
import androidx.room.Entity
import androidx.room.PrimaryKey

object ScriptAlignment {
    const val START = 0
    const val CENTER = 1
    const val END = 2

    fun toTextAlign(value: Int): TextAlign = when (value) {
        START -> TextAlign.Start
        END -> TextAlign.End
        else -> TextAlign.Center
    }
}

enum class ScriptSortMode {
    ALPHABETICAL,
    RECENT,
    CUSTOM
}

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val content: String,
    val folderName: String = DEFAULT_FOLDER,
    val scrollSpeed: Float = DEFAULT_SCROLL_SPEED,
    val fontSizeSp: Float = DEFAULT_FONT_SIZE_SP,
    val textAlignment: Int = ScriptAlignment.CENTER,
    val horizontalMarginDp: Float = DEFAULT_MARGIN_DP,
    val sortOrder: Int = 0,
    val sourceUri: String? = null,
    val sourceFileName: String? = null,
    val linkedFolderUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_FOLDER = "General"
        const val DEFAULT_SCROLL_SPEED = 8.0f
        const val MIN_SCROLL_SPEED = 0.5f
        const val MAX_SCROLL_SPEED = 50.0f
        const val DEFAULT_FONT_SIZE_SP = 46f
        const val MIN_FONT_SIZE_SP = 20f
        const val MAX_FONT_SIZE_SP = 90f
        const val DEFAULT_MARGIN_DP = 64f
        const val MIN_MARGIN_DP = 0f
        const val MAX_MARGIN_DP = 240f
    }
}

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val name: String,
    val linkedTreeUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
