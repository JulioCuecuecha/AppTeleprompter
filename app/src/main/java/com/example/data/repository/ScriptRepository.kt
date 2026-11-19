package com.example.data.repository

import android.net.Uri
import com.example.data.local.PrompterPreferences
import com.example.data.local.ScriptDao
import com.example.data.local.StageSettings
import com.example.data.model.FolderEntity
import com.example.data.model.ScriptEntity
import com.example.data.model.ScriptSortMode
import com.example.data.sync.FolderSyncManager
import com.example.data.sync.SyncReport
import kotlinx.coroutines.flow.Flow

class ScriptRepository(
    private val scriptDao: ScriptDao,
    private val preferences: PrompterPreferences,
    private val folderSyncManager: FolderSyncManager
) {
    val allScripts: Flow<List<ScriptEntity>> = scriptDao.observeAllScripts()
    val allFolders: Flow<List<FolderEntity>> = scriptDao.observeAllFolders()
    val stageSettings: Flow<StageSettings> = preferences.settingsFlow

    suspend fun saveScript(
        id: Long?,
        title: String,
        content: String,
        folderName: String,
        scrollSpeed: Float,
        fontSizeSp: Float,
        textAlignment: Int,
        horizontalMarginDp: Float
    ): Long {
        val cleanTitle = title.trim().ifEmpty { "Sin título" }
        val cleanFolder = folderName.trim().ifEmpty { ScriptEntity.DEFAULT_FOLDER }
        scriptDao.insertFolder(FolderEntity(name = cleanFolder))

        return if (id != null && id > 0L) {
            val existing = scriptDao.getScriptById(id)
            if (existing != null) {
                scriptDao.updateScript(
                    existing.copy(
                        title = cleanTitle,
                        content = content,
                        folderName = cleanFolder,
                        scrollSpeed = scrollSpeed.coerceIn(
                            ScriptEntity.MIN_SCROLL_SPEED,
                            ScriptEntity.MAX_SCROLL_SPEED
                        ),
                        fontSizeSp = fontSizeSp.coerceIn(
                            ScriptEntity.MIN_FONT_SIZE_SP,
                            ScriptEntity.MAX_FONT_SIZE_SP
                        ),
                        textAlignment = textAlignment,
                        horizontalMarginDp = horizontalMarginDp.coerceIn(
                            ScriptEntity.MIN_MARGIN_DP,
                            ScriptEntity.MAX_MARGIN_DP
                        ),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                id
            } else {
                val nextOrder = scriptDao.getNextSortOrder()
                scriptDao.insertScript(
                    ScriptEntity(
                        title = cleanTitle,
                        content = content,
                        folderName = cleanFolder,
                        scrollSpeed = scrollSpeed,
                        fontSizeSp = fontSizeSp,
                        textAlignment = textAlignment,
                        horizontalMarginDp = horizontalMarginDp,
                        sortOrder = nextOrder
                    )
                )
            }
        } else {
            val nextOrder = scriptDao.getNextSortOrder()
            scriptDao.insertScript(
                ScriptEntity(
                    title = cleanTitle,
                    content = content,
                    folderName = cleanFolder,
                    scrollSpeed = scrollSpeed,
                    fontSizeSp = fontSizeSp,
                    textAlignment = textAlignment,
                    horizontalMarginDp = horizontalMarginDp,
                    sortOrder = nextOrder
                )
            )
        }
    }

    suspend fun updatePlaybackSettings(
        scriptId: Long,
        scrollSpeed: Float,
        fontSizeSp: Float,
        textAlignment: Int,
        horizontalMarginDp: Float
    ) {
        scriptDao.updatePlaybackSettings(
            id = scriptId,
            scrollSpeed = scrollSpeed.coerceIn(
                ScriptEntity.MIN_SCROLL_SPEED,
                ScriptEntity.MAX_SCROLL_SPEED
            ),
            fontSizeSp = fontSizeSp.coerceIn(
                ScriptEntity.MIN_FONT_SIZE_SP,
                ScriptEntity.MAX_FONT_SIZE_SP
            ),
            textAlignment = textAlignment,
            horizontalMarginDp = horizontalMarginDp.coerceIn(
                ScriptEntity.MIN_MARGIN_DP,
                ScriptEntity.MAX_MARGIN_DP
            )
        )
    }

    suspend fun deleteScript(id: Long) {
        scriptDao.deleteScriptById(id)
    }

    suspend fun createFolder(name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) {
            scriptDao.insertFolder(FolderEntity(name = clean))
        }
    }

    suspend fun persistSortOrder(orderedIds: List<Long>) {
        scriptDao.updateSortOrders(orderedIds)
    }

    suspend fun linkAndSyncFolder(treeUri: Uri): Result<SyncReport> =
        folderSyncManager.linkAndSyncFolder(treeUri)

    suspend fun syncLinkedFolder(treeUriString: String): Result<SyncReport> =
        folderSyncManager.syncLinkedFolder(treeUriString)

    suspend fun importTxtFiles(uris: List<Uri>, targetFolder: String): Result<SyncReport> =
        folderSyncManager.importTxtFiles(uris, targetFolder)

    suspend fun importPromptBackup(uri: Uri): Result<SyncReport> =
        folderSyncManager.importPromptBackup(uri)

    suspend fun setCountdownSeconds(seconds: Int) = preferences.setCountdownSeconds(seconds)
    suspend fun setReadingGuideOpacity(opacity: Float) = preferences.setReadingGuideOpacity(opacity)
    suspend fun setMirrorHorizontal(enabled: Boolean) = preferences.setMirrorHorizontal(enabled)
    suspend fun setMirrorVertical(enabled: Boolean) = preferences.setMirrorVertical(enabled)
    suspend fun setSortMode(mode: ScriptSortMode) = preferences.setSortMode(mode)
}
