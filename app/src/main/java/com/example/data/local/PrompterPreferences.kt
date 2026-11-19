package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ScriptSortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.prompterDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "stageprompt_preferences"
)

data class StageSettings(
    val linkedFolderUri: String? = null,
    val linkedFolderName: String? = null,
    val lastSyncTimestamp: Long = 0L,
    val countdownSeconds: Int = 3,
    val readingGuideOpacity: Float = 0.28f,
    val mirrorHorizontal: Boolean = false,
    val mirrorVertical: Boolean = false,
    val sortMode: ScriptSortMode = ScriptSortMode.ALPHABETICAL
)

class PrompterPreferences(private val context: Context) {

    private object Keys {
        val LINKED_FOLDER_URI = stringPreferencesKey("linked_folder_uri")
        val LINKED_FOLDER_NAME = stringPreferencesKey("linked_folder_name")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val COUNTDOWN_SECONDS = intPreferencesKey("countdown_seconds")
        val READING_GUIDE_OPACITY = floatPreferencesKey("reading_guide_opacity")
        val MIRROR_HORIZONTAL = booleanPreferencesKey("mirror_horizontal")
        val MIRROR_VERTICAL = booleanPreferencesKey("mirror_vertical")
        val SORT_MODE = stringPreferencesKey("sort_mode")
    }

    val settingsFlow: Flow<StageSettings> = context.prompterDataStore.data.map { prefs ->
        val sortModeRaw = prefs[Keys.SORT_MODE] ?: ScriptSortMode.ALPHABETICAL.name
        val sortMode = runCatching { ScriptSortMode.valueOf(sortModeRaw) }
            .getOrDefault(ScriptSortMode.ALPHABETICAL)

        StageSettings(
            linkedFolderUri = prefs[Keys.LINKED_FOLDER_URI],
            linkedFolderName = prefs[Keys.LINKED_FOLDER_NAME],
            lastSyncTimestamp = prefs[Keys.LAST_SYNC_TIMESTAMP] ?: 0L,
            countdownSeconds = (prefs[Keys.COUNTDOWN_SECONDS] ?: 3).let {
                if (it in setOf(0, 3, 5)) it else 3
            },
            readingGuideOpacity = (prefs[Keys.READING_GUIDE_OPACITY] ?: 0.28f).coerceIn(0f, 0.75f),
            mirrorHorizontal = prefs[Keys.MIRROR_HORIZONTAL] ?: false,
            mirrorVertical = prefs[Keys.MIRROR_VERTICAL] ?: false,
            sortMode = sortMode
        )
    }

    suspend fun saveLinkedFolder(uri: String, displayName: String) {
        context.prompterDataStore.edit { prefs ->
            prefs[Keys.LINKED_FOLDER_URI] = uri
            prefs[Keys.LINKED_FOLDER_NAME] = displayName
            prefs[Keys.LAST_SYNC_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long = System.currentTimeMillis()) {
        context.prompterDataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    suspend fun setCountdownSeconds(seconds: Int) {
        context.prompterDataStore.edit { prefs ->
            prefs[Keys.COUNTDOWN_SECONDS] = seconds
        }
    }

    suspend fun setReadingGuideOpacity(opacity: Float) {
        context.prompterDataStore.edit { prefs ->
            prefs[Keys.READING_GUIDE_OPACITY] = opacity.coerceIn(0f, 0.75f)
        }
    }

    suspend fun setMirrorHorizontal(enabled: Boolean) {
        context.prompterDataStore.edit { prefs ->
            prefs[Keys.MIRROR_HORIZONTAL] = enabled
        }
    }

    suspend fun setMirrorVertical(enabled: Boolean) {
        context.prompterDataStore.edit { prefs ->
            prefs[Keys.MIRROR_VERTICAL] = enabled
        }
    }

    suspend fun setSortMode(mode: ScriptSortMode) {
        context.prompterDataStore.edit { prefs ->
            prefs[Keys.SORT_MODE] = mode.name
        }
    }
}
