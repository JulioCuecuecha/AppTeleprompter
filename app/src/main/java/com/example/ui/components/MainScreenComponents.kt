package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ScriptAlignment
import com.example.data.model.ScriptEntity
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.BroadcastAmberContainer
import com.example.ui.theme.CueMusicCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnBroadcastAmber
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageBorderSubtle
import com.example.ui.theme.StageDanger
import com.example.ui.theme.StageObsidian
import com.example.ui.theme.StagePureBlack
import com.example.ui.theme.StageSuccess
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary
import com.example.ui.utils.ScriptTagHighlighter
import com.example.ui.viewmodel.PrompterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FolderSyncBanner(
    linkedFolderName: String?,
    lastSyncTimestamp: Long,
    isSyncing: Boolean,
    onLinkFolder: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = StageSurface,
        border = BorderStroke(
            1.dp,
            if (linkedFolderName != null) StageSuccess.copy(alpha = 0.35f) else StageBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (linkedFolderName != null) StageSuccess.copy(alpha = 0.14f)
                        else BroadcastAmberContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (linkedFolderName != null) Icons.Default.CloudDone else Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = if (linkedFolderName != null) StageSuccess else BroadcastAmber,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (linkedFolderName != null) {
                        stringResource(R.string.folder_linked_label, linkedFolderName)
                    } else {
                        stringResource(R.string.no_folder_linked)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = StageTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (linkedFolderName != null && lastSyncTimestamp > 0L) {
                    val formatted = remember(lastSyncTimestamp) {
                        SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
                            .format(Date(lastSyncTimestamp))
                    }
                    Text(
                        text = "Última sincronización: $formatted",
                        style = MaterialTheme.typography.labelSmall,
                        color = StageTextSecondary
                    )
                } else {
                    Text(
                        text = "Permiso persistente SAF para archivos .TXT",
                        style = MaterialTheme.typography.bodySmall,
                        color = StageTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (linkedFolderName != null) {
                Button(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    modifier = Modifier.testTag("sync_folder_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StageSurfaceElevated,
                        contentColor = CueMusicCyan
                    ),
                    border = BorderStroke(1.dp, CueMusicCyan.copy(alpha = 0.45f))
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = CueMusicCyan
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = stringResource(R.string.sync_folder),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isSyncing) stringResource(R.string.syncing_folder) else "Sincronizar",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                TextButton(
                    onClick = onLinkFolder,
                    modifier = Modifier.testTag("link_folder_quick_button")
                ) {
                    Text(
                        text = "Vincular",
                        color = BroadcastAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ImportModeBar(
    onLinkFolder: () -> Unit,
    onImportTxtFiles: () -> Unit,
    onImportPromptBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ImportActionPill(
            icon = Icons.Default.FolderOpen,
            label = "Carpeta .TXT",
            accent = BroadcastAmber,
            onClick = onLinkFolder,
            modifier = Modifier
                .weight(1f)
                .testTag("import_link_folder_button")
        )
        ImportActionPill(
            icon = Icons.Default.Description,
            label = "Archivos .TXT",
            accent = CueMusicCyan,
            onClick = onImportTxtFiles,
            modifier = Modifier
                .weight(1f)
                .testTag("import_txt_files_button")
        )
        ImportActionPill(
            icon = Icons.Default.Restore,
            label = ".prompt / JSON",
            accent = Color(0xFFFF5C8D),
            onClick = onImportPromptBackup,
            modifier = Modifier
                .weight(1f)
                .testTag("import_prompt_backup_button")
        )
    }
}

@Composable
private fun ImportActionPill(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = StageSurface,
        border = BorderStroke(1.dp, StageBorder)
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
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = StageTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ScriptListItemCard(
    script: ScriptEntity,
    isSelected: Boolean,
    isCustomSortMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onQuickPlay: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) StageSurfaceElevated else StageObsidian
    val borderColor = if (isSelected) BroadcastAmber else StageBorderSubtle

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("script_item_${script.id}"),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = isCustomSortMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = dragHandleModifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StageSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Arrastrar para reordenar",
                            tint = BroadcastAmber
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }

            // Left active cue indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) BroadcastAmber else StageBorder)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = script.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = StageTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = script.folderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = CueMusicCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .background(CueMusicCyan.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "VEL ${PrompterViewModel.formatSpeed(script.scrollSpeed)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StageTextSecondary
                    )
                    Text(
                        text = "${script.fontSizeSp.toInt()}sp",
                        style = MaterialTheme.typography.labelSmall,
                        color = StageTextMuted
                    )
                }
            }

            if (isCustomSortMode) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.testTag("move_up_${script.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Subir posición",
                        tint = if (canMoveUp) StageTextPrimary else StageTextMuted
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.testTag("move_down_${script.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Bajar posición",
                        tint = if (canMoveDown) StageTextPrimary else StageTextMuted
                    )
                }
            } else {
                IconButton(
                    onClick = onQuickPlay,
                    modifier = Modifier.testTag("quick_play_${script.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.start_teleprompter),
                        tint = if (isSelected) BroadcastAmber else StageTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ScriptDetailPreviewPane(
    script: ScriptEntity,
    onStartTeleprompter: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val highlightedPreview = remember(script.content) {
        ScriptTagHighlighter.highlight(script.content)
    }
    val lineCount = remember(script.content) {
        script.content.lines().count { it.isNotBlank() }
    }
    val wordCount = remember(script.content) {
        script.content.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StagePureBlack)
            .padding(20.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showBackButton) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, StageBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.back_to_list),
                        color = StageTextPrimary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = script.folderName.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = BroadcastAmber,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = script.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = StageTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("preview_edit_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StageBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_script),
                    tint = StageTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.edit_script),
                    color = StageTextPrimary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, StageBorder, RoundedCornerShape(12.dp))
                    .testTag("preview_delete_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete_script),
                    tint = StageDanger
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Per-song telemetry chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryStatBadge(
                icon = Icons.Default.Speed,
                label = "Velocidad",
                value = PrompterViewModel.formatSpeed(script.scrollSpeed),
                modifier = Modifier.weight(1f)
            )
            TelemetryStatBadge(
                icon = Icons.Default.TextFields,
                label = "Fuente",
                value = "${script.fontSizeSp.toInt()} sp",
                modifier = Modifier.weight(1f)
            )
            TelemetryStatBadge(
                icon = when (script.textAlignment) {
                    ScriptAlignment.START -> Icons.AutoMirrored.Filled.FormatAlignLeft
                    ScriptAlignment.END -> Icons.AutoMirrored.Filled.FormatAlignRight
                    else -> Icons.Default.FormatAlignCenter
                },
                label = "Márgenes",
                value = "${script.horizontalMarginDp.toInt()} dp",
                modifier = Modifier.weight(1f)
            )
            TelemetryStatBadge(
                icon = Icons.Default.Description,
                label = "Extensión",
                value = "$wordCount pal · $lineCount lín",
                modifier = Modifier.weight(1.2f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // High-contrast script preview window
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = StageObsidian,
            border = BorderStroke(1.dp, StageBorder)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 22.dp)
                ) {
                    if (script.content.isBlank()) {
                        Text(
                            text = "Este guion aún no tiene contenido. Toca «Editar» para añadir la letra o texto.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = StageTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp)
                        )
                    } else {
                        Text(
                            text = highlightedPreview,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp,
                                lineHeight = 36.sp
                            ),
                            color = StageTextPrimary,
                            textAlign = ScriptAlignment.toTextAlign(script.textAlignment),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(48.dp))
                }

                // Bottom fade gradient
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, StageObsidian)
                            )
                        )
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // GIANT Stage Launch Button
        Button(
            onClick = onStartTeleprompter,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .testTag("start_teleprompter_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BroadcastAmber,
                contentColor = OnBroadcastAmber
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.start_teleprompter),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
            )
        }
    }
}

@Composable
private fun TelemetryStatBadge(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = StageSurface,
        border = BorderStroke(1.dp, StageBorderSubtle)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = StageTextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = StageTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = StageTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyStageDetailPane(
    hasScriptsInLibrary: Boolean,
    onLinkFolder: () -> Unit,
    onImportTxtFiles: () -> Unit,
    onImportPromptBackup: () -> Unit,
    onCreateNewScript: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StagePureBlack)
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_stage_empty_1795124504443),
            contentDescription = stringResource(R.string.empty_repertoire_title),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(190.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, StageBorderSubtle, RoundedCornerShape(20.dp))
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = if (hasScriptsInLibrary) {
                "Vista previa lista"
            } else {
                stringResource(R.string.empty_repertoire_title)
            },
            style = MaterialTheme.typography.headlineMedium,
            color = StageTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = if (hasScriptsInLibrary) {
                stringResource(R.string.select_script_prompt)
            } else {
                stringResource(R.string.empty_repertoire_subtitle)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = StageTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(Modifier.height(26.dp))

        Button(
            onClick = onLinkFolder,
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(54.dp)
                .testTag("empty_link_folder_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BroadcastAmber,
                contentColor = OnBroadcastAmber
            )
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.link_txt_folder),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.78f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onImportTxtFiles,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StageBorder)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = CueMusicCyan)
                Spacer(Modifier.width(6.dp))
                Text("Archivos .TXT", color = StageTextPrimary, maxLines = 1)
            }
            OutlinedButton(
                onClick = onImportPromptBackup,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StageBorder)
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, tint = Color(0xFFFF5C8D))
                Spacer(Modifier.width(6.dp))
                Text(".prompt", color = StageTextPrimary, maxLines = 1)
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = onCreateNewScript,
            modifier = Modifier.testTag("empty_new_script_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = BroadcastAmber)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.new_script),
                color = BroadcastAmber,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StageSurfaceElevated,
        icon = {
            Icon(
                imageVector = Icons.Default.CreateNewFolder,
                contentDescription = null,
                tint = BroadcastAmber
            )
        },
        title = {
            Text(
                text = stringResource(R.string.create_folder_title),
                color = StageTextPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                singleLine = true,
                placeholder = {
                    Text(
                        text = stringResource(R.string.folder_name_placeholder),
                        color = StageTextMuted
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BroadcastAmber,
                    unfocusedBorderColor = StageBorder,
                    focusedTextColor = StageTextPrimary,
                    unfocusedTextColor = StageTextPrimary,
                    cursorColor = BroadcastAmber
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_folder_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(folderName) },
                enabled = folderName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BroadcastAmber,
                    contentColor = OnBroadcastAmber
                ),
                modifier = Modifier.testTag("confirm_create_folder_button")
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = StageTextSecondary)
            }
        }
    )
}

@Composable
fun DeleteScriptDialog(
    script: ScriptEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StageSurfaceElevated,
        title = {
            Text(
                text = stringResource(R.string.delete_confirm_title),
                color = StageTextPrimary
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_confirm_message, script.title),
                color = StageTextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StageDanger,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_delete_script_button")
            ) {
                Text(stringResource(R.string.delete_script))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = StageTextSecondary)
            }
        }
    )
}
