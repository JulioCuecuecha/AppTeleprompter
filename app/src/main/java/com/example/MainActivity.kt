package com.example

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ScriptEntity
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.PrompterPlayerScreen
import com.example.ui.theme.StagePromptTheme
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.PrompterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PrompterViewModel by viewModels()

    // Active hardware keyboard / Bluetooth pedal interceptor when PrompterPlayerScreen is visible
    private var activeHardwareKeyHandler: ((KeyEvent) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK)
        )

        setContent {
            StagePromptTheme {
                val destination by viewModel.destination.collectAsStateWithLifecycle()
                val managerUiState by viewModel.managerUiState.collectAsStateWithLifecycle()
                val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

                when (val dest = destination) {
                    is AppDestination.Manager -> {
                        MainScreen(
                            uiState = managerUiState,
                            onSearchQueryChanged = viewModel::onSearchQueryChanged,
                            onFolderSelected = viewModel::onFolderSelected,
                            onSortModeSelected = viewModel::onSortModeSelected,
                            onSelectScript = viewModel::selectScript,
                            onReorderScript = viewModel::reorderScript,
                            onStartPlayer = viewModel::openPlayer,
                            onCreateNewScript = viewModel::openNewEditor,
                            onEditScript = viewModel::openEditor,
                            onDeleteScript = viewModel::deleteScript,
                            onCreateFolder = viewModel::createFolder,
                            onLinkFolderUri = viewModel::onFolderPickedForSync,
                            onSyncLinkedFolder = viewModel::syncLinkedFolderNow,
                            onImportMultipleTxt = viewModel::onMultipleTxtFilesPicked,
                            onImportPromptBackup = viewModel::onPromptBackupPicked,
                            onClearStatusMessage = viewModel::clearStatusMessage
                        )
                    }

                    is AppDestination.Editor -> {
                        val existing = viewModel.getScriptForEditor(dest.scriptId)
                        EditorScreen(
                            existingScript = existing,
                            availableFolders = managerUiState.folders,
                            defaultFolder = managerUiState.selectedFolder ?: ScriptEntity.DEFAULT_FOLDER,
                            onBack = viewModel::navigateBackToManager,
                            onSave = viewModel::saveScript
                        )
                    }

                    is AppDestination.Player -> {
                        PrompterPlayerScreen(
                            state = playbackState,
                            onExit = viewModel::navigateBackToManager,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onPause = viewModel::pausePlayback,
                            onReachedEnd = viewModel::onPlaybackReachedEnd,
                            onUpdateProgress = viewModel::updateScrollProgress,
                            onSetHudVisible = viewModel::setHudVisible,
                            onToggleAppearanceSheet = viewModel::toggleAppearanceSheet,
                            onSpeedChanged = { speed -> viewModel.updateLiveSpeed(speed) },
                            onNudgeSpeed = viewModel::nudgeLiveSpeed,
                            onFontSizeChanged = viewModel::updateLiveFontSize,
                            onMarginChanged = viewModel::updateLiveHorizontalMargin,
                            onAlignmentChanged = viewModel::updateLiveAlignment,
                            onCountdownChanged = viewModel::updateCountdownSeconds,
                            onGuideOpacityChanged = viewModel::updateReadingGuideOpacity,
                            onToggleMirrorH = viewModel::toggleMirrorHorizontal,
                            onToggleMirrorV = viewModel::toggleMirrorVertical,
                            onAdjacentSong = viewModel::openAdjacentScriptInPlayer,
                            onShowTransientBadge = viewModel::showTransientBadge,
                            registerHardwareKeyHandler = { handler ->
                                activeHardwareKeyHandler = handler
                            }
                        )
                    }
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handler = activeHardwareKeyHandler
        if (handler != null && handler(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
