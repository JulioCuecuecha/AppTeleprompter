package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ScriptAlignment
import com.example.data.model.ScriptEntity
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.BroadcastAmberContainer
import com.example.ui.theme.CueChorusCoral
import com.example.ui.theme.CueChorusCoralBg
import com.example.ui.theme.CueMusicCyan
import com.example.ui.theme.CueMusicCyanBg
import com.example.ui.theme.CueVocalEmerald
import com.example.ui.theme.CueVocalEmeraldBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnBroadcastAmber
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageBorderSubtle
import com.example.ui.theme.StageObsidian
import com.example.ui.theme.StagePureBlack
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary
import com.example.ui.utils.ScriptCueVisualTransformation
import com.example.ui.utils.ScriptTagHighlighter
import com.example.ui.viewmodel.PrompterViewModel

@Composable
fun EditorScreen(
    existingScript: ScriptEntity?,
    availableFolders: List<String>,
    defaultFolder: String,
    onBack: () -> Unit,
    onSave: (
        id: Long?,
        title: String,
        content: String,
        folderName: String,
        scrollSpeed: Float,
        fontSizeSp: Float,
        textAlignment: Int,
        horizontalMarginDp: Float
    ) -> Unit
) {
    BackHandler(onBack = onBack)

    var title by remember(existingScript?.id) { mutableStateOf(existingScript?.title ?: "") }
    var contentField by remember(existingScript?.id) {
        val text = existingScript?.content ?: ""
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }
    var selectedFolder by remember(existingScript?.id) {
        mutableStateOf(existingScript?.folderName ?: defaultFolder)
    }
    var scrollSpeed by remember(existingScript?.id) {
        mutableFloatStateOf(existingScript?.scrollSpeed ?: ScriptEntity.DEFAULT_SCROLL_SPEED)
    }
    var fontSizeSp by remember(existingScript?.id) {
        mutableFloatStateOf(existingScript?.fontSizeSp ?: ScriptEntity.DEFAULT_FONT_SIZE_SP)
    }
    var textAlignment by remember(existingScript?.id) {
        mutableIntStateOf(existingScript?.textAlignment ?: ScriptAlignment.CENTER)
    }
    val horizontalMarginDp = existingScript?.horizontalMarginDp ?: ScriptEntity.DEFAULT_MARGIN_DP

    var folderDropdownExpanded by remember { mutableStateOf(false) }
    val folderOptions = remember(availableFolders, selectedFolder) {
        (availableFolders + ScriptEntity.DEFAULT_FOLDER + selectedFolder)
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    val cueVisualTransformation = remember { ScriptCueVisualTransformation() }

    fun insertCueTagAtCursor(tag: String) {
        val currentText = contentField.text
        val start = contentField.selection.min.coerceIn(0, currentText.length)
        val end = contentField.selection.max.coerceIn(0, currentText.length)
        val prefixSpace = if (start > 0 && currentText[start - 1] != '\n' && currentText[start - 1] != ' ') "\n" else ""
        val suffixSpace = if (end < currentText.length && currentText[end] != '\n') "\n" else "\n"
        val inserted = "$prefixSpace$tag$suffixSpace"
        val newText = currentText.substring(0, start) + inserted + currentText.substring(end)
        val newCursor = start + inserted.length
        contentField = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = StagePureBlack,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StagePureBlack)
        ) {
            // Top Editor Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StageObsidian)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.discard_changes),
                        tint = StageTextPrimary
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.script_title_label),
                            color = StageTextMuted
                        )
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
                        .weight(1f)
                        .testTag("editor_title_input")
                )

                // Folder Selector Dropdown
                Box {
                    Surface(
                        onClick = { folderDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = StageSurface,
                        border = BorderStroke(1.dp, StageBorder),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("editor_folder_selector")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = stringResource(R.string.folder_label),
                                tint = CueMusicCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = selectedFolder,
                                style = MaterialTheme.typography.labelLarge,
                                color = StageTextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = StageTextSecondary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = folderDropdownExpanded,
                        onDismissRequest = { folderDropdownExpanded = false },
                        containerColor = StageSurfaceElevated
                    ) {
                        folderOptions.forEach { folder ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = folder,
                                        color = if (folder == selectedFolder) BroadcastAmber else StageTextPrimary
                                    )
                                },
                                onClick = {
                                    selectedFolder = folder
                                    folderDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onSave(
                            existingScript?.id,
                            title,
                            contentField.text,
                            selectedFolder,
                            scrollSpeed,
                            fontSizeSp,
                            textAlignment,
                            horizontalMarginDp
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BroadcastAmber,
                        contentColor = OnBroadcastAmber
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("editor_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.save_script),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = StageBorderSubtle)

            // Quick Stage Cue Tag Insertion Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StageObsidian)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.quick_tags_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = StageTextSecondary
                )
                Spacer(Modifier.width(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 12.dp)
                ) {
                    items(ScriptTagHighlighter.QuickCueTags) { tag ->
                        val (accent, bg) = when {
                            tag.startsWith("[") -> CueMusicCyan to CueMusicCyanBg
                            tag.contains("Coro", ignoreCase = true) -> CueChorusCoral to CueChorusCoralBg
                            else -> CueVocalEmerald to CueVocalEmeraldBg
                        }
                        Surface(
                            onClick = { insertCueTagAtCursor(tag) },
                            shape = RoundedCornerShape(10.dp),
                            color = bg,
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("quick_tag_$tag")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tag,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = accent
                                )
                            }
                        }
                    }
                }
            }

            // Per-Song Speed, Font & Alignment Quick Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StageSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Speed slider
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VEL ${PrompterViewModel.formatSpeed(scrollSpeed)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = BroadcastAmber,
                        modifier = Modifier.width(68.dp)
                    )
                    Slider(
                        value = scrollSpeed,
                        onValueChange = { scrollSpeed = it },
                        valueRange = ScriptEntity.MIN_SCROLL_SPEED..ScriptEntity.MAX_SCROLL_SPEED,
                        colors = SliderDefaults.colors(
                            thumbColor = BroadcastAmber,
                            activeTrackColor = BroadcastAmber,
                            inactiveTrackColor = StageBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Font size slider
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${fontSizeSp.toInt()}sp",
                        style = MaterialTheme.typography.labelMedium,
                        color = CueMusicCyan,
                        modifier = Modifier.width(46.dp)
                    )
                    Slider(
                        value = fontSizeSp,
                        onValueChange = { fontSizeSp = it },
                        valueRange = ScriptEntity.MIN_FONT_SIZE_SP..ScriptEntity.MAX_FONT_SIZE_SP,
                        colors = SliderDefaults.colors(
                            thumbColor = CueMusicCyan,
                            activeTrackColor = CueMusicCyan,
                            inactiveTrackColor = StageBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Alignment toggle
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    EditorAlignButton(
                        selected = textAlignment == ScriptAlignment.START,
                        icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                        contentDescription = stringResource(R.string.align_left),
                        onClick = { textAlignment = ScriptAlignment.START }
                    )
                    EditorAlignButton(
                        selected = textAlignment == ScriptAlignment.CENTER,
                        icon = Icons.Default.FormatAlignCenter,
                        contentDescription = stringResource(R.string.align_center),
                        onClick = { textAlignment = ScriptAlignment.CENTER }
                    )
                    EditorAlignButton(
                        selected = textAlignment == ScriptAlignment.END,
                        icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                        contentDescription = stringResource(R.string.align_right),
                        onClick = { textAlignment = ScriptAlignment.END }
                    )
                }
            }

            HorizontalDivider(color = StageBorderSubtle)

            // Full-Screen Live Color-Highlighted Script Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(StagePureBlack)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                if (contentField.text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.script_content_placeholder),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 22.sp,
                            lineHeight = 34.sp
                        ),
                        color = StageTextMuted
                    )
                }

                BasicTextField(
                    value = contentField,
                    onValueChange = { contentField = it },
                    visualTransformation = cueVisualTransformation,
                    textStyle = TextStyle(
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 24.sp,
                        lineHeight = 36.sp,
                        color = StageTextPrimary,
                        textAlign = ScriptAlignment.toTextAlign(textAlignment)
                    ),
                    cursorBrush = SolidColor(BroadcastAmber),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("editor_content_input")
                )
            }
        }
    }
}

@Composable
private fun EditorAlignButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) BroadcastAmberContainer else StageObsidian,
        border = BorderStroke(1.dp, if (selected) BroadcastAmber else StageBorderSubtle),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) BroadcastAmber else StageTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
