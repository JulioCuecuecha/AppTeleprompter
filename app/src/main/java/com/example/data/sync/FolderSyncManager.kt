package com.example.data.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.PrompterPreferences
import com.example.data.local.ScriptDao
import com.example.data.model.FolderEntity
import com.example.data.model.ScriptEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class SyncReport(
    val folderName: String,
    val addedCount: Int,
    val updatedCount: Int,
    val unchangedCount: Int,
    val skippedDeletedCount: Int = 0
) {
    val totalProcessed: Int get() = addedCount + updatedCount + unchangedCount
}

/**
 * Manages Storage Access Framework (SAF) persistent folder linking, incremental `.txt`
 * synchronization without overwriting per-song speed/font settings, single/multi `.txt`
 * import, and Teleprompter Pro `.prompt`/`.json` backup restoration.
 */
class FolderSyncManager(
    private val context: Context,
    private val scriptDao: ScriptDao,
    private val preferences: PrompterPreferences
) {

    /**
     * Takes persistent read URI permission for the selected folder and performs a full sync.
     */
    suspend fun linkAndSyncFolder(treeUri: Uri): Result<SyncReport> = withContext(Dispatchers.IO) {
        runCatching {
            takePersistableReadPermission(treeUri)
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: throw IllegalStateException("No se pudo acceder a la carpeta seleccionada.")

            val rootFolderName = rootDoc.name?.trim()?.ifEmpty { "Carpeta TXT" } ?: "Carpeta TXT"
            preferences.saveLinkedFolder(uri = treeUri.toString(), displayName = rootFolderName)
            scriptDao.insertFolder(
                FolderEntity(
                    name = rootFolderName,
                    linkedTreeUri = treeUri.toString()
                )
            )
            syncDocumentTree(
                rootDoc = rootDoc,
                treeUriString = treeUri.toString(),
                rootFolderName = rootFolderName
            )
        }
    }

    /**
     * Re-scans the previously linked SAF folder to pick up new `.txt` songs or edited lyrics
     * without duplicating existing scripts or resetting saved speed/font/margin/alignment.
     */
    suspend fun syncLinkedFolder(linkedTreeUriString: String): Result<SyncReport> =
        withContext(Dispatchers.IO) {
            runCatching {
                val treeUri = Uri.parse(linkedTreeUriString)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                    ?: throw IllegalStateException("La carpeta vinculada ya no está disponible. Vuelve a vincularla.")
                if (!rootDoc.exists() || !rootDoc.canRead()) {
                    throw IllegalStateException("Permiso revocado o carpeta inaccesible. Vuelve a vincular la carpeta.")
                }

                val rootFolderName = rootDoc.name?.trim()?.ifEmpty { "Carpeta TXT" } ?: "Carpeta TXT"
                val report = syncDocumentTree(
                    rootDoc = rootDoc,
                    treeUriString = linkedTreeUriString,
                    rootFolderName = rootFolderName
                )
                preferences.updateLastSyncTimestamp()
                report
            }
        }

    /**
     * Imports one or multiple standalone `.txt` files selected via `OpenMultipleDocuments`.
     */
    suspend fun importTxtFiles(
        uris: List<Uri>,
        targetFolder: String = ScriptEntity.DEFAULT_FOLDER
    ): Result<SyncReport> = withContext(Dispatchers.IO) {
        runCatching {
            if (uris.isEmpty()) {
                return@runCatching SyncReport(targetFolder, 0, 0, 0)
            }
            scriptDao.insertFolder(FolderEntity(name = targetFolder))

            var added = 0
            var updated = 0
            var unchanged = 0
            var nextSortOrder = scriptDao.getNextSortOrder()

            for (uri in uris) {
                val doc = DocumentFile.fromSingleUri(context, uri)
                val fileName = doc?.name ?: uri.lastPathSegment ?: "guion.txt"
                val title = stripTxtExtension(fileName)
                val content = readUtf8Text(uri) ?: continue

                val existing = scriptDao.findBySourceUri(uri.toString())
                    ?: scriptDao.findByTitleAndFolder(title, targetFolder)

                if (existing != null) {
                    if (existing.content != content || existing.title != title) {
                        scriptDao.updateScript(
                            existing.copy(
                                title = title,
                                content = content,
                                sourceUri = uri.toString(),
                                sourceFileName = fileName,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        updated++
                    } else {
                        unchanged++
                    }
                } else {
                    scriptDao.insertScript(
                        ScriptEntity(
                            title = title,
                            content = content,
                            folderName = targetFolder,
                            sortOrder = nextSortOrder++,
                            sourceUri = uri.toString(),
                            sourceFileName = fileName
                        )
                    )
                    added++
                }
            }

            SyncReport(
                folderName = targetFolder,
                addedCount = added,
                updatedCount = updated,
                unchangedCount = unchanged
            )
        }
    }

    /**
     * Imports a Teleprompter Pro `.prompt` or `.json` backup file, restoring each active
     * song's original `scrollSpeed` and `fontSize`.
     */
    suspend fun importPromptBackup(uri: Uri): Result<SyncReport> = withContext(Dispatchers.IO) {
        runCatching {
            val rawJson = readUtf8Text(uri)
                ?: throw IllegalStateException("No se pudo leer el archivo de respaldo.")

            val parsed = PromptBackupParser.parse(rawJson)
            var added = 0
            var updated = 0
            var unchanged = 0
            var nextSortOrder = scriptDao.getNextSortOrder()

            for (item in parsed.scripts) {
                scriptDao.insertFolder(FolderEntity(name = item.folderName))
                val existing = scriptDao.findByTitleAndFolder(item.title, item.folderName)
                    ?: scriptDao.findByTitle(item.title)

                if (existing != null) {
                    if (existing.content != item.content ||
                        existing.scrollSpeed != item.scrollSpeed ||
                        existing.fontSizeSp != item.fontSizeSp
                    ) {
                        scriptDao.updateScript(
                            existing.copy(
                                content = item.content,
                                scrollSpeed = item.scrollSpeed,
                                fontSizeSp = item.fontSizeSp,
                                folderName = item.folderName,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        updated++
                    } else {
                        unchanged++
                    }
                } else {
                    scriptDao.insertScript(
                        ScriptEntity(
                            title = item.title,
                            content = item.content,
                            folderName = item.folderName,
                            scrollSpeed = item.scrollSpeed,
                            fontSizeSp = item.fontSizeSp,
                            sortOrder = nextSortOrder++
                        )
                    )
                    added++
                }
            }

            SyncReport(
                folderName = PromptBackupParser.DEFAULT_IMPORT_FOLDER,
                addedCount = added,
                updatedCount = updated,
                unchangedCount = unchanged,
                skippedDeletedCount = parsed.skippedDeletedCount
            )
        }
    }

    private suspend fun syncDocumentTree(
        rootDoc: DocumentFile,
        treeUriString: String,
        rootFolderName: String
    ): SyncReport {
        var added = 0
        var updated = 0
        var unchanged = 0
        var nextSortOrder = scriptDao.getNextSortOrder()

        suspend fun processDirectory(dir: DocumentFile, folderLabel: String, depth: Int) {
            scriptDao.insertFolder(
                FolderEntity(
                    name = folderLabel,
                    linkedTreeUri = if (depth == 0) treeUriString else null
                )
            )
            val children = dir.listFiles()
            for (child in children) {
                if (child.isDirectory && depth < 2) {
                    val subName = child.name?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith(".") }
                    if (subName != null) {
                        processDirectory(child, subName, depth + 1)
                    }
                    continue
                }
                if (!child.isFile) continue
                val fileName = child.name ?: continue
                if (!fileName.endsWith(".txt", ignoreCase = true)) continue

                val title = stripTxtExtension(fileName)
                val content = readUtf8Text(child.uri) ?: continue

                // Preserve existing speed, font size, alignment, margins & sortOrder!
                val existing = scriptDao.findByLinkedFolderAndFileName(treeUriString, fileName)
                    ?: scriptDao.findBySourceUri(child.uri.toString())
                    ?: scriptDao.findByTitleAndFolder(title, folderLabel)

                if (existing != null) {
                    if (existing.content != content || existing.title != title || existing.folderName != folderLabel) {
                        scriptDao.updateScript(
                            existing.copy(
                                title = title,
                                content = content,
                                folderName = folderLabel,
                                sourceUri = child.uri.toString(),
                                sourceFileName = fileName,
                                linkedFolderUri = treeUriString,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        updated++
                    } else {
                        unchanged++
                    }
                } else {
                    scriptDao.insertScript(
                        ScriptEntity(
                            title = title,
                            content = content,
                            folderName = folderLabel,
                            sortOrder = nextSortOrder++,
                            sourceUri = child.uri.toString(),
                            sourceFileName = fileName,
                            linkedFolderUri = treeUriString
                        )
                    )
                    added++
                }
            }
        }

        processDirectory(rootDoc, rootFolderName, depth = 0)

        return SyncReport(
            folderName = rootFolderName,
            addedCount = added,
            updatedCount = updated,
            unchangedCount = unchanged
        )
    }

    private fun takePersistableReadPermission(treeUri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (_: SecurityException) {
            // Some document providers only grant temporary read permission; continue safely.
        }
    }

    private fun readUtf8Text(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                        .removePrefix("\uFEFF")
                        .replace("\r\n", "\n")
                        .replace("\r", "\n")
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun stripTxtExtension(fileName: String): String {
        val cleaned = fileName.trim()
        return if (cleaned.endsWith(".txt", ignoreCase = true)) {
            cleaned.dropLast(4).trim().ifEmpty { cleaned }
        } else {
            cleaned.ifEmpty { "Sin título" }
        }
    }
}
