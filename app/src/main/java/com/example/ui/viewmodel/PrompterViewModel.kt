package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PrompterDatabase
import com.example.data.local.PrompterPreferences
import com.example.data.local.StageSettings
import com.example.data.model.ScriptEntity
import com.example.data.model.ScriptSortMode
import com.example.data.repository.ScriptRepository
import com.example.data.sync.FolderSyncManager
import com.example.data.sync.SyncReport
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed interface AppDestination {
    data object Manager : AppDestination
    data class Editor(val scriptId: Long?) : AppDestination
    data class Player(val scriptId: Long) : AppDestination
}

data class PlaybackUiState(
    val scriptId: Long = 0L,
    val title: String = "",
    val content: String = "",
    val isPlaying: Boolean = false,
    val countdownRemaining: Int = 0,
    val scrollSpeed: Float = ScriptEntity.DEFAULT_SCROLL_SPEED,
    val fontSizeSp: Float = ScriptEntity.DEFAULT_FONT_SIZE_SP,
    val horizontalMarginDp: Float = ScriptEntity.DEFAULT_MARGIN_DP,
    val textAlignment: Int = com.example.data.model.ScriptAlignment.CENTER,
    val mirrorHorizontal: Boolean = false,
    val mirrorVertical: Boolean = false,
    val countdownSeconds: Int = 3,
    val readingGuideOpacity: Float = 0.28f,
    val isHudVisible: Boolean = true,
    val isAppearanceSheetOpen: Boolean = false,
    val scrollProgress: Float = 0f,
    val liveFeedbackBadge: String? = null
)

data class ManagerUiState(
    val scripts: List<ScriptEntity> = emptyList(),
    val totalScriptCount: Int = 0,
    val folders: List<String> = emptyList(),
    val selectedFolder: String? = null,
    val searchQuery: String = "",
    val sortMode: ScriptSortMode = ScriptSortMode.ALPHABETICAL,
    val selectedScriptId: Long? = null,
    val linkedFolderUri: String? = null,
    val linkedFolderName: String? = null,
    val lastSyncTimestamp: Long = 0L,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null,
    val isStatusError: Boolean = false
) {
    val selectedScript: ScriptEntity?
        get() = scripts.firstOrNull { it.id == selectedScriptId } ?: scripts.firstOrNull()
}

private data class FilterState(
    val searchQuery: String = "",
    val selectedFolder: String? = null,
    val selectedScriptId: Long? = null,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null,
    val isStatusError: Boolean = false
)

class PrompterViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PrompterDatabase.getInstance(application)
    private val preferences = PrompterPreferences(application)
    private val folderSyncManager = FolderSyncManager(
        context = application,
        scriptDao = database.scriptDao(),
        preferences = preferences
    )
    private val repository = ScriptRepository(
        scriptDao = database.scriptDao(),
        preferences = preferences,
        folderSyncManager = folderSyncManager
    )

    private val _destination = MutableStateFlow<AppDestination>(AppDestination.Manager)
    val destination: StateFlow<AppDestination> = _destination.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val latestScripts = MutableStateFlow<List<ScriptEntity>>(emptyList())
    private val latestSettings = MutableStateFlow(StageSettings())

    private var savePlaybackSettingsJob: Job? = null
    private var countdownJob: Job? = null
    private var feedbackBadgeJob: Job? = null

    val managerUiState: StateFlow<ManagerUiState> = combine(
        repository.allScripts,
        repository.allFolders,
        repository.stageSettings,
        _filterState
    ) { allScripts, storedFolders, settings, filter ->
        latestScripts.value = allScripts
        latestSettings.value = settings

        val folderNames = (storedFolders.map { it.name } + allScripts.map { it.folderName })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

        val activeFolder = filter.selectedFolder?.takeIf { it in folderNames }
        val query = filter.searchQuery.trim()

        val filtered = allScripts.filter { script ->
            val matchesFolder = activeFolder == null ||
                script.folderName.equals(activeFolder, ignoreCase = true)
            val matchesQuery = query.isEmpty() ||
                script.title.contains(query, ignoreCase = true) ||
                script.content.contains(query, ignoreCase = true) ||
                script.folderName.contains(query, ignoreCase = true)
            matchesFolder && matchesQuery
        }

        val sorted = when (settings.sortMode) {
            ScriptSortMode.ALPHABETICAL -> filtered.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            ScriptSortMode.RECENT -> filtered.sortedByDescending { it.updatedAt }
            ScriptSortMode.CUSTOM -> filtered.sortedWith(
                compareBy<ScriptEntity> { it.sortOrder }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
        }

        val resolvedSelection = filter.selectedScriptId
            ?.takeIf { id -> sorted.any { it.id == id } }
            ?: sorted.firstOrNull()?.id

        ManagerUiState(
            scripts = sorted,
            totalScriptCount = allScripts.size,
            folders = folderNames,
            selectedFolder = activeFolder,
            searchQuery = filter.searchQuery,
            sortMode = settings.sortMode,
            selectedScriptId = resolvedSelection,
            linkedFolderUri = settings.linkedFolderUri,
            linkedFolderName = settings.linkedFolderName,
            lastSyncTimestamp = settings.lastSyncTimestamp,
            isSyncing = filter.isSyncing,
            statusMessage = filter.statusMessage,
            isStatusError = filter.isStatusError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ManagerUiState()
    )

    // --- Navigation ---

    fun selectScript(scriptId: Long) {
        _filterState.update { it.copy(selectedScriptId = scriptId) }
    }

    fun openNewEditor() {
        _destination.value = AppDestination.Editor(scriptId = null)
    }

    fun openEditor(scriptId: Long) {
        _destination.value = AppDestination.Editor(scriptId = scriptId)
    }

    fun openPlayer(script: ScriptEntity) {
        countdownJob?.cancel()
        val settings = latestSettings.value
        _filterState.update { it.copy(selectedScriptId = script.id) }
        _playbackState.value = PlaybackUiState(
            scriptId = script.id,
            title = script.title,
            content = script.content,
            isPlaying = false,
            countdownRemaining = 0,
            scrollSpeed = script.scrollSpeed,
            fontSizeSp = script.fontSizeSp,
            horizontalMarginDp = script.horizontalMarginDp,
            textAlignment = script.textAlignment,
            mirrorHorizontal = settings.mirrorHorizontal,
            mirrorVertical = settings.mirrorVertical,
            countdownSeconds = settings.countdownSeconds,
            readingGuideOpacity = settings.readingGuideOpacity,
            isHudVisible = true,
            isAppearanceSheetOpen = false,
            scrollProgress = 0f,
            liveFeedbackBadge = null
        )
        _destination.value = AppDestination.Player(scriptId = script.id)
    }

    fun navigateBackToManager() {
        countdownJob?.cancel()
        _playbackState.update { it.copy(isPlaying = false, countdownRemaining = 0) }
        _destination.value = AppDestination.Manager
    }

    fun getScriptForEditor(scriptId: Long?): ScriptEntity? {
        if (scriptId == null) return null
        return latestScripts.value.firstOrNull { it.id == scriptId }
    }

    // --- Filtering, Sorting & Custom Drag-and-Drop Reorder ---

    fun onSearchQueryChanged(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun onFolderSelected(folder: String?) {
        _filterState.update { it.copy(selectedFolder = folder) }
    }

    fun onSortModeSelected(mode: ScriptSortMode) {
        viewModelScope.launch {
            repository.setSortMode(mode)
        }
    }

    fun reorderScript(fromIndex: Int, toIndex: Int) {
        val currentList = managerUiState.value.scripts
        if (fromIndex == toIndex ||
            fromIndex !in currentList.indices ||
            toIndex !in currentList.indices
        ) {
            return
        }

        val mutableVisible = currentList.toMutableList()
        val moved = mutableVisible.removeAt(fromIndex)
        mutableVisible.add(toIndex, moved)

        // Merge reordered visible subset back into full list order
        val visibleIdSet = mutableVisible.map { it.id }.toSet()
        val reorderedQueue = ArrayDeque(mutableVisible.map { it.id })
        val mergedIds = latestScripts.value
            .sortedBy { it.sortOrder }
            .map { script ->
                if (script.id in visibleIdSet) {
                    reorderedQueue.removeFirst()
                } else {
                    script.id
                }
            }

        viewModelScope.launch {
            repository.setSortMode(ScriptSortMode.CUSTOM)
            repository.persistSortOrder(mergedIds)
        }
    }

    // --- CRUD ---

    fun saveScript(
        id: Long?,
        title: String,
        content: String,
        folderName: String,
        scrollSpeed: Float,
        fontSizeSp: Float,
        textAlignment: Int,
        horizontalMarginDp: Float
    ) {
        viewModelScope.launch {
            val savedId = repository.saveScript(
                id = id,
                title = title,
                content = content,
                folderName = folderName,
                scrollSpeed = scrollSpeed,
                fontSizeSp = fontSizeSp,
                textAlignment = textAlignment,
                horizontalMarginDp = horizontalMarginDp
            )
            _filterState.update {
                it.copy(
                    selectedScriptId = savedId,
                    statusMessage = "Guion «${title.trim().ifEmpty { "Sin título" }}» guardado.",
                    isStatusError = false
                )
            }
            _destination.value = AppDestination.Manager
        }
    }

    fun deleteScript(script: ScriptEntity) {
        viewModelScope.launch {
            repository.deleteScript(script.id)
            _filterState.update {
                it.copy(
                    selectedScriptId = if (it.selectedScriptId == script.id) null else it.selectedScriptId,
                    statusMessage = "Guion «${script.title}» eliminado.",
                    isStatusError = false
                )
            }
        }
    }

    fun createFolder(folderName: String) {
        val clean = folderName.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.createFolder(clean)
            _filterState.update {
                it.copy(
                    selectedFolder = clean,
                    statusMessage = "Carpeta «$clean» creada.",
                    isStatusError = false
                )
            }
        }
    }

    fun clearStatusMessage() {
        _filterState.update { it.copy(statusMessage = null, isStatusError = false) }
    }

    // --- SAF Folder Sync & Imports ---

    fun onFolderPickedForSync(treeUri: Uri) {
        viewModelScope.launch {
            _filterState.update { it.copy(isSyncing = true, statusMessage = null) }
            val result = repository.linkAndSyncFolder(treeUri)
            handleSyncResult(result, prefix = "Carpeta vinculada")
        }
    }

    fun syncLinkedFolderNow() {
        val uriString = managerUiState.value.linkedFolderUri
        if (uriString.isNullOrBlank()) {
            _filterState.update {
                it.copy(
                    statusMessage = "Primero vincula una carpeta de archivos .TXT.",
                    isStatusError = true
                )
            }
            return
        }
        viewModelScope.launch {
            _filterState.update { it.copy(isSyncing = true, statusMessage = null) }
            val result = repository.syncLinkedFolder(uriString)
            handleSyncResult(result, prefix = "Sincronización completada")
        }
    }

    fun onMultipleTxtFilesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val targetFolder = managerUiState.value.selectedFolder ?: ScriptEntity.DEFAULT_FOLDER
        viewModelScope.launch {
            _filterState.update { it.copy(isSyncing = true, statusMessage = null) }
            val result = repository.importTxtFiles(uris, targetFolder)
            handleSyncResult(result, prefix = "Importación .TXT")
        }
    }

    fun onPromptBackupPicked(uri: Uri) {
        viewModelScope.launch {
            _filterState.update { it.copy(isSyncing = true, statusMessage = null) }
            val result = repository.importPromptBackup(uri)
            handleSyncResult(result, prefix = "Respaldo .prompt importado")
        }
    }

    private fun handleSyncResult(result: Result<SyncReport>, prefix: String) {
        result.fold(
            onSuccess = { report ->
                val summary = buildString {
                    append("$prefix («${report.folderName}»): ")
                    append("${report.addedCount} nuevos, ${report.updatedCount} actualizados")
                    if (report.unchangedCount > 0) {
                        append(", ${report.unchangedCount} sin cambios")
                    }
                    if (report.skippedDeletedCount > 0) {
                        append(" (${report.skippedDeletedCount} eliminados omitidos)")
                    }
                }
                _filterState.update {
                    it.copy(
                        isSyncing = false,
                        statusMessage = summary,
                        isStatusError = false
                    )
                }
            },
            onFailure = { error ->
                _filterState.update {
                    it.copy(
                        isSyncing = false,
                        statusMessage = error.localizedMessage ?: "Error al procesar los archivos.",
                        isStatusError = true
                    )
                }
            }
        )
    }

    // --- Live Teleprompter Playback Controls ---

    fun togglePlayPause() {
        val current = _playbackState.value
        if (current.countdownRemaining > 0) {
            // Cancel active countdown
            countdownJob?.cancel()
            _playbackState.update {
                it.copy(countdownRemaining = 0, isPlaying = false, isHudVisible = true)
            }
            return
        }

        if (current.isPlaying) {
            _playbackState.update {
                it.copy(isPlaying = false, isHudVisible = true)
            }
            showLiveBadge("PAUSA")
        } else {
            startPlaybackWithOptionalCountdown()
        }
    }

    fun pausePlayback() {
        countdownJob?.cancel()
        _playbackState.update {
            it.copy(isPlaying = false, countdownRemaining = 0, isHudVisible = true)
        }
    }

    private fun startPlaybackWithOptionalCountdown() {
        val current = _playbackState.value
        val shouldCountdown = current.countdownSeconds > 0 && current.scrollProgress < 0.02f
        if (!shouldCountdown) {
            _playbackState.update {
                it.copy(isPlaying = true, countdownRemaining = 0, isAppearanceSheetOpen = false)
            }
            showLiveBadge("PLAY · ${formatSpeed(current.scrollSpeed)}")
            return
        }

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _playbackState.update {
                it.copy(isHudVisible = false, isAppearanceSheetOpen = false)
            }
            for (sec in current.countdownSeconds downTo 1) {
                _playbackState.update { it.copy(countdownRemaining = sec) }
                delay(1_000L)
            }
            _playbackState.update {
                it.copy(countdownRemaining = 0, isPlaying = true)
            }
        }
    }

    fun onPlaybackReachedEnd() {
        _playbackState.update {
            it.copy(isPlaying = false, isHudVisible = true, scrollProgress = 1f)
        }
    }

    fun updateScrollProgress(progress: Float) {
        _playbackState.update { it.copy(scrollProgress = progress.coerceIn(0f, 1f)) }
    }

    fun setHudVisible(visible: Boolean) {
        _playbackState.update {
            it.copy(
                isHudVisible = visible,
                isAppearanceSheetOpen = if (!visible) false else it.isAppearanceSheetOpen
            )
        }
    }

    fun toggleAppearanceSheet() {
        _playbackState.update {
            it.copy(
                isHudVisible = true,
                isAppearanceSheetOpen = !it.isAppearanceSheetOpen
            )
        }
    }

    fun updateLiveSpeed(newSpeed: Float, showBadge: Boolean = false) {
        val clamped = (newSpeed * 2f).roundToInt() / 2f
        val safeSpeed = clamped.coerceIn(
            ScriptEntity.MIN_SCROLL_SPEED,
            ScriptEntity.MAX_SCROLL_SPEED
        )
        _playbackState.update { it.copy(scrollSpeed = safeSpeed) }
        if (showBadge) {
            showLiveBadge("VELOCIDAD ${formatSpeed(safeSpeed)}")
        }
        schedulePersistPlaybackSettings()
    }

    fun nudgeLiveSpeed(delta: Float) {
        updateLiveSpeed(_playbackState.value.scrollSpeed + delta, showBadge = true)
    }

    fun updateLiveFontSize(newSizeSp: Float) {
        val safeSize = newSizeSp.roundToInt().toFloat().coerceIn(
            ScriptEntity.MIN_FONT_SIZE_SP,
            ScriptEntity.MAX_FONT_SIZE_SP
        )
        _playbackState.update { it.copy(fontSizeSp = safeSize) }
        schedulePersistPlaybackSettings()
    }

    fun updateLiveHorizontalMargin(marginDp: Float) {
        val safeMargin = marginDp.roundToInt().toFloat().coerceIn(
            ScriptEntity.MIN_MARGIN_DP,
            ScriptEntity.MAX_MARGIN_DP
        )
        _playbackState.update { it.copy(horizontalMarginDp = safeMargin) }
        schedulePersistPlaybackSettings()
    }

    fun updateLiveAlignment(alignment: Int) {
        _playbackState.update { it.copy(textAlignment = alignment) }
        schedulePersistPlaybackSettings()
    }

    fun updateCountdownSeconds(seconds: Int) {
        _playbackState.update { it.copy(countdownSeconds = seconds) }
        viewModelScope.launch { repository.setCountdownSeconds(seconds) }
        showLiveBadge(if (seconds == 0) "SIN CUENTA" else "CUENTA ${seconds}s")
    }

    fun updateReadingGuideOpacity(opacity: Float) {
        val safe = opacity.coerceIn(0f, 0.75f)
        _playbackState.update { it.copy(readingGuideOpacity = safe) }
        viewModelScope.launch { repository.setReadingGuideOpacity(safe) }
    }

    fun toggleMirrorHorizontal() {
        val next = !_playbackState.value.mirrorHorizontal
        _playbackState.update { it.copy(mirrorHorizontal = next) }
        viewModelScope.launch { repository.setMirrorHorizontal(next) }
        showLiveBadge(if (next) "ESPEJO H: ON" else "ESPEJO H: OFF")
    }

    fun toggleMirrorVertical() {
        val next = !_playbackState.value.mirrorVertical
        _playbackState.update { it.copy(mirrorVertical = next) }
        viewModelScope.launch { repository.setMirrorVertical(next) }
        showLiveBadge(if (next) "ESPEJO V: ON" else "ESPEJO V: OFF")
    }

    fun openAdjacentScriptInPlayer(direction: Int) {
        val list = managerUiState.value.scripts
        if (list.size <= 1) return
        val currentIndex = list.indexOfFirst { it.id == _playbackState.value.scriptId }
        if (currentIndex == -1) return
        val nextIndex = (currentIndex + direction).coerceIn(0, list.lastIndex)
        if (nextIndex != currentIndex) {
            openPlayer(list[nextIndex])
        }
    }

    fun showTransientBadge(message: String) {
        showLiveBadge(message)
    }

    private fun showLiveBadge(message: String) {
        feedbackBadgeJob?.cancel()
        _playbackState.update { it.copy(liveFeedbackBadge = message) }
        feedbackBadgeJob = viewModelScope.launch {
            delay(1_400L)
            _playbackState.update { it.copy(liveFeedbackBadge = null) }
        }
    }

    private fun schedulePersistPlaybackSettings() {
        savePlaybackSettingsJob?.cancel()
        savePlaybackSettingsJob = viewModelScope.launch {
            delay(300L)
            val state = _playbackState.value
            if (state.scriptId > 0L) {
                repository.updatePlaybackSettings(
                    scriptId = state.scriptId,
                    scrollSpeed = state.scrollSpeed,
                    fontSizeSp = state.fontSizeSp,
                    textAlignment = state.textAlignment,
                    horizontalMarginDp = state.horizontalMarginDp
                )
            }
        }
    }

    companion object {
        fun formatSpeed(speed: Float): String {
            return if (speed % 1f == 0f) {
                speed.toInt().toString()
            } else {
                String.format(java.util.Locale.US, "%.1f", speed)
            }
        }
    }
}
