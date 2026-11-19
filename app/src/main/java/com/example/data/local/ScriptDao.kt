package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.FolderEntity
import com.example.data.model.ScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {

    @Query("SELECT * FROM scripts ORDER BY sortOrder ASC, title COLLATE NOCASE ASC")
    fun observeAllScripts(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    fun observeAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    suspend fun getScriptById(id: Long): ScriptEntity?

    @Query("SELECT * FROM scripts WHERE linkedFolderUri = :folderUri AND sourceFileName = :fileName LIMIT 1")
    suspend fun findByLinkedFolderAndFileName(folderUri: String, fileName: String): ScriptEntity?

    @Query("SELECT * FROM scripts WHERE sourceUri = :sourceUri LIMIT 1")
    suspend fun findBySourceUri(sourceUri: String): ScriptEntity?

    @Query("SELECT * FROM scripts WHERE LOWER(title) = LOWER(:title) AND LOWER(folderName) = LOWER(:folderName) LIMIT 1")
    suspend fun findByTitleAndFolder(title: String, folderName: String): ScriptEntity?

    @Query("SELECT * FROM scripts WHERE LOWER(title) = LOWER(:title) LIMIT 1")
    suspend fun findByTitle(title: String): ScriptEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM scripts")
    suspend fun getNextSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptEntity): Long

    @Update
    suspend fun updateScript(script: ScriptEntity)

    @Query("UPDATE scripts SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun updateSortOrders(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            updateSortOrder(id = id, sortOrder = index)
        }
    }

    @Query(
        """
        UPDATE scripts 
        SET scrollSpeed = :scrollSpeed,
            fontSizeSp = :fontSizeSp,
            textAlignment = :textAlignment,
            horizontalMarginDp = :horizontalMarginDp,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updatePlaybackSettings(
        id: Long,
        scrollSpeed: Float,
        fontSizeSp: Float,
        textAlignment: Int,
        horizontalMarginDp: Float,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteScriptById(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("UPDATE scripts SET folderName = :newFolder WHERE folderName = :oldFolder")
    suspend fun reassignFolder(oldFolder: String, newFolder: String)

    @Query("DELETE FROM folders WHERE name = :name")
    suspend fun deleteFolderByName(name: String)
}
