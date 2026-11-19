package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.model.ScriptEntity
import com.example.data.model.ScriptSortMode
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.DeleteScriptDialog
import com.example.ui.components.EmptyStageDetailPane
import com.example.ui.components.FolderSyncBanner
import com.example.ui.components.ImportModeBar
import com.example.ui.components.ScriptDetailPreviewPane
import com.example.ui.components.ScriptListItemCard
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.BroadcastAmberContainer
import com.example.ui.theme.CueMusicCyan
import com.example.ui.theme.OnBroadcastAmber
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageBorderSubtle
import com.example.ui.theme.StageDanger
import com.example.ui.theme.StageDangerContainer
import com.example.ui.theme.StageObsidian
import com.example.ui.theme.StagePureBlack
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary
import com.example.ui.viewmodel.ManagerUiState

@Composable
fun MainScreen(
    uiState: ManagerUiState,
    onSearchQueryChanged: (String) -> Unit,
    onFolderSelected: (String?) -> Unit,
    onSortModeSelected: (ScriptSortMode) -> Unit,
    onSelectScript: (Long) -> Unit,
    onReorderScript: (Int, Int) -> Unit,
    onStartPlayer: (ScriptEntity) -> Unit,
    onCreateNewScript: () -> Unit,
    onEditScript: (Long) -> Unit,
    onDeleteScript: (ScriptEntity) -> Unit,
    onCreateFolder: (String) -> Unit,
    onLinkFolderUri: (Uri) -> Unit,
    onSyncLinkedFolder: () -> Unit,
    onImportMultipleTxt: (List<Uri>) -> Unit,
    onImportPromptBackup: (Uri) -> Unit,
    onClearStatusMessage: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var scriptPendingDelete by remember { mutableStateOf<ScriptEntity?>(null) }
    var showCompactDetail by remember { mutableStateOf(false) }

    // 1. SAF Persistent Folder Picker (.TXT folder sync)
    val folderTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            onLinkFolderUri(treeUri)
        }
    }

    // 2. Multi-file .TXT Importer
    val multipleTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportMultipleTxt(uris)
        }
    }

    // 3. Teleprompter Pro (.prompt / .json) Backup Importer
    val promptBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportPromptBackup(uri)
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Short
        )
        onClearStatusMessage()
    }

    val launchLinkFolder = { folderTreeLauncher.launch(null) }
    val launchImportTxt = { multipleTxtLauncher.launch(arrayOf("text/plain", "text/*")) }
    val launchImportPrompt = {
        promptBackupLauncher.launch(arrayOf("*/*"))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = StagePureBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (uiState.isStatusError) StageDangerContainer else StageSurfaceElevated,
                    contentColor = if (uiState.isStatusError) StageDanger else StageTextPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StagePureBlack)
        ) {
            val isWideTablet = maxWidth >= 760.dp

            if (isWideTablet) {
                // Landscape Tablet Master-Detail (Galaxy Tab S8+ 12.4" primary experience)
                Row(modifier = Modifier.fillMaxSize()) {
                    MasterRepertoirePane(
                        uiState = uiState,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onFolderSelected = onFolderSelected,
                        onSortModeSelected = onSortModeSelected,
                        onSelectScript = onSelectScript,
                        onQuickPlay = onStartPlayer,
                        onReorderScript = onReorderScript,
                        onCreateNewScript = onCreateNewScript,
                        onOpenCreateFolder = { showCreateFolderDialog = true },
                        onLinkFolder = launchLinkFolder,
                        onSyncLinkedFolder = onSyncLinkedFolder,
                        onImportTxtFiles = launchImportTxt,
                        onImportPromptBackup = launchImportPrompt,
                        modifier = Modifier
                            .weight(0.43f)
                            .fillMaxHeight()
                    )

                    VerticalDivider(
                        thickness = 1.dp,
                        color = StageBorderSubtle
                    )

                    val activeScript = uiState.selectedScript
                    if (activeScript != null) {
                        ScriptDetailPreviewPane(
                            script = activeScript,
                            onStartTeleprompter = { onStartPlayer(activeScript) },
                            onEdit = { onEditScript(activeScript.id) },
                            onDelete = { scriptPendingDelete = activeScript },
                            modifier = Modifier
                                .weight(0.57f)
                                .fillMaxHeight()
                        )
                    } else {
                        EmptyStageDetailPane(
                            hasScriptsInLibrary = uiState.totalScriptCount > 0,
                            onLinkFolder = launchLinkFolder,
                            onImportTxtFiles = launchImportTxt,
                            onImportPromptBackup = launchImportPrompt,
                            onCreateNewScript = onCreateNewScript,
                            modifier = Modifier
                                .weight(0.57f)
                                .fillMaxHeight()
                        )
                    }
                }
            } else {
                // Compact Portrait Phone / Split-Screen Adaptive Mode
                val activeScript = uiState.selectedScript
                if (showCompactDetail && activeScript != null) {
                    BackHandler { showCompactDetail = false }
                    ScriptDetailPreviewPane(
                        script = activeScript,
                        onStartTeleprompter = { onStartPlayer(activeScript) },
                        onEdit = { onEditScript(activeScript.id) },
                        onDelete = { scriptPendingDelete = activeScript },
                        showBackButton = true,
                        onBack = { showCompactDetail = false },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MasterRepertoirePane(
                        uiState = uiState,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onFolderSelected = onFolderSelected,
                        onSortModeSelected = onSortModeSelected,
                        onSelectScript = { id ->
                            onSelectScript(id)
                            showCompactDetail = true
                        },
                        onQuickPlay = onStartPlayer,
                        onReorderScript = onReorderScript,
                        onCreateNewScript = onCreateNewScript,
                        onOpenCreateFolder = { showCreateFolderDialog = true },
                        onLinkFolder = launchLinkFolder,
                        onSyncLinkedFolder = onSyncLinkedFolder,
                        onImportTxtFiles = launchImportTxt,
                        onImportPromptBackup = launchImportPrompt,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { name ->
                onCreateFolder(name)
                showCreateFolderDialog = false
            }
        )
    }

    scriptPendingDelete?.let { script ->
        DeleteScriptDialog(
            script = script,
            onDismiss = { scriptPendingDelete = null },
            onConfirm = {
                onDeleteScript(script)
                scriptPendingDelete = null
                showCompactDetail = false
            }
        )
    }
}

@Composable
private fun MasterRepertoirePane(
    uiState: ManagerUiState,
    onSearchQueryChanged: (String) -> Unit,
    onFolderSelected: (String?) -> Unit,
    onSortModeSelected: (ScriptSortMode) -> Unit,
    onSelectScript: (Long) -> Unit,
    onQuickPlay: (ScriptEntity) -> Unit,
    onReorderScript: (Int, Int) -> Unit,
    onCreateNewScript: () -> Unit,
    onOpenCreateFolder: () -> Unit,
    onLinkFolder: () -> Unit,
    onSyncLinkedFolder: () -> Unit,
    onImportTxtFiles: () -> Unit,
    onImportPromptBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var draggedItemId by remember { mutableStateOf<Long?>(null) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .background(StageObsidian)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Top Brand Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BroadcastAmberContainer,
                        border = BorderStroke(1.dp, BroadcastAmber.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = null,
                                tint = BroadcastAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = StageTextPrimary
                        )
                        Text(
                            text = stringResource(R.string.scripts_count_format, uiState.totalScriptCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = StageTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onOpenCreateFolder,
                    modifier = Modifier.testTag("create_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = stringResource(R.string.new_folder),
                        tint = StageTextSecondary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Linked SAF Folder Sync Status Banner
            FolderSyncBanner(
                linkedFolderName = uiState.linkedFolderName,
                lastSyncTimestamp = uiState.lastSyncTimestamp,
                isSyncing = uiState.isSyncing,
                onLinkFolder = onLinkFolder,
                onSyncNow = onSyncLinkedFolder
            )

            Spacer(Modifier.height(10.dp))

            // 3-Mode Import Bar (.TXT Folder, Multi .TXT, .prompt/.json)
            ImportModeBar(
                onLinkFolder = onLinkFolder,
                onImportTxtFiles = onImportTxtFiles,
                onImportPromptBackup = onImportPromptBackup
            )

            Spacer(Modifier.height(12.dp))

            // Instant Search Field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                singleLine = true,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_scripts_placeholder),
                        color = StageTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = StageTextSecondary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChanged("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = StageTextSecondary
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = StageSurface,
                    unfocusedContainerColor = StageSurface,
                    focusedBorderColor = BroadcastAmber,
                    unfocusedBorderColor = StageBorderSubtle,
                    focusedTextColor = StageTextPrimary,
                    unfocusedTextColor = StageTextPrimary,
                    cursorColor = BroadcastAmber
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_scripts_input")
            )

            Spacer(Modifier.height(10.dp))

            // Folder / Setlist Filter Strip
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "all_folders") {
                    FilterChip(
                        selected = uiState.selectedFolder == null,
                        onClick = { onFolderSelected(null) },
                        label = { Text(stringResource(R.string.all_folders)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BroadcastAmberContainer,
                            selectedLabelColor = BroadcastAmber,
                            containerColor = StageSurface,
                            labelColor = StageTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = uiState.selectedFolder == null,
                            borderColor = StageBorderSubtle,
                            selectedBorderColor = BroadcastAmber
                        ),
                        modifier = Modifier.testTag("folder_chip_all")
                    )
                }

                items(uiState.folders, key = { it }) { folder ->
                    val isSelected = uiState.selectedFolder.equals(folder, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFolderSelected(if (isSelected) null else folder) },
                        label = { Text(folder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CueMusicCyan.copy(alpha = 0.16f),
                            selectedLabelColor = CueMusicCyan,
                            containerColor = StageSurface,
                            labelColor = StageTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = StageBorderSubtle,
                            selectedBorderColor = CueMusicCyan
                        ),
                        modifier = Modifier.testTag("folder_chip_$folder")
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Sort Mode Bar (A-Z, Reciente, Personalizado Drag & Drop)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SortChip(
                    label = stringResource(R.string.sort_alphabetical),
                    icon = Icons.Default.SortByAlpha,
                    selected = uiState.sortMode == ScriptSortMode.ALPHABETICAL,
                    onClick = { onSortModeSelected(ScriptSortMode.ALPHABETICAL) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sort_alphabetical_chip")
                )
                SortChip(
                    label = stringResource(R.string.sort_recent),
                    icon = Icons.Default.Schedule,
                    selected = uiState.sortMode == ScriptSortMode.RECENT,
                    onClick = { onSortModeSelected(ScriptSortMode.RECENT) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sort_recent_chip")
                )
                SortChip(
                    label = stringResource(R.string.sort_custom),
                    icon = Icons.Default.SwapVert,
                    selected = uiState.sortMode == ScriptSortMode.CUSTOM,
                    onClick = { onSortModeSelected(ScriptSortMode.CUSTOM) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sort_custom_chip")
                )
            }

            AnimatedVisibility(visible = uiState.sortMode == ScriptSortMode.CUSTOM && uiState.scripts.size > 1) {
                Text(
                    text = stringResource(R.string.drag_reorder_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = BroadcastAmber,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = StageBorderSubtle)
            Spacer(Modifier.height(10.dp))

            // Scripts List
            if (uiState.scripts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.totalScriptCount == 0) {
                            "Aún no hay guiones. Importa tu carpeta .TXT o toca «+» para crear uno."
                        } else {
                            "No se encontraron coincidencias para tu búsqueda o filtro."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = StageTextMuted,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("scripts_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 84.dp)
                ) {
                    itemsIndexed(
                        items = uiState.scripts,
                        key = { _, item -> item.id }
                    ) { index, script ->
                        val isDragging = draggedItemId == script.id
                        val isCustom = uiState.sortMode == ScriptSortMode.CUSTOM

                        val dragHandleModifier = if (isCustom) {
                            Modifier.pointerInput(script.id, uiState.scripts.size) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggedItemId = script.id
                                        dragDeltaY = 0f
                                    },
                                    onDragEnd = {
                                        draggedItemId = null
                                        dragDeltaY = 0f
                                    },
                                    onDragCancel = {
                                        draggedItemId = null
                                        dragDeltaY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragDeltaY += dragAmount.y
                                        val thresholdPx = 68f
                                        val currentIdx = uiState.scripts.indexOfFirst { it.id == script.id }
                                        if (currentIdx != -1) {
                                            if (dragDeltaY > thresholdPx && currentIdx < uiState.scripts.lastIndex) {
                                                onReorderScript(currentIdx, currentIdx + 1)
                                                dragDeltaY -= thresholdPx
                                            } else if (dragDeltaY < -thresholdPx && currentIdx > 0) {
                                                onReorderScript(currentIdx, currentIdx - 1)
                                                dragDeltaY += thresholdPx
                                            }
                                        }
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }

                        ScriptListItemCard(
                            script = script,
                            isSelected = uiState.selectedScriptId == script.id,
                            isCustomSortMode = isCustom,
                            canMoveUp = index > 0,
                            canMoveDown = index < uiState.scripts.lastIndex,
                            onClick = { onSelectScript(script.id) },
                            onQuickPlay = { onQuickPlay(script) },
                            onMoveUp = { onReorderScript(index, index - 1) },
                            onMoveDown = { onReorderScript(index, index + 1) },
                            dragHandleModifier = dragHandleModifier,
                            modifier = Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragDeltaY else 0f
                                    scaleX = if (isDragging) 1.02f else 1f
                                    scaleY = if (isDragging) 1.02f else 1f
                                    alpha = if (isDragging) 0.94f else 1f
                                }
                        )
                    }
                }
            }
        }

        // FAB to create new script
        FloatingActionButton(
            onClick = onCreateNewScript,
            containerColor = BroadcastAmber,
            contentColor = OnBroadcastAmber,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(18.dp)
                .testTag("new_script_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.new_script)
            )
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) BroadcastAmberContainer else StageSurface,
        border = BorderStroke(
            1.dp,
            if (selected) BroadcastAmber else StageBorderSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) BroadcastAmber else StageTextMuted,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) BroadcastAmber else StageTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
