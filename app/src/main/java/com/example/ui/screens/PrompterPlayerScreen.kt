package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent as AndroidKeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.R
import com.example.data.model.ScriptAlignment
import com.example.data.model.ScriptEntity
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.BroadcastAmberContainer
import com.example.ui.theme.CueMusicCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnBroadcastAmber
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageObsidian
import com.example.ui.theme.StagePureBlack
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary
import com.example.ui.theme.StageWhite
import com.example.ui.utils.ScriptTagHighlighter
import com.example.ui.viewmodel.PlaybackUiState
import com.example.ui.viewmodel.PrompterViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen pure-OLED (#000000) Live Teleprompter Stage Player.
 *
 * Features:
 * - True sub-pixel 60/120 FPS continuous scrolling via `withFrameNanos` + GPU `graphicsLayer`.
 * - Full hardware keyboard & Bluetooth pedal control (Space/Enter, Up/Down, PageUp/PageDown).
 * - Auto-hiding HUD with live speed (0.5–50), font size (20–90sp), margins, reading guide opacity,
 *   H/V mirror mode, configurable 0/3/5s countdown, and `FLAG_KEEP_SCREEN_ON`.
 */
@Composable
fun PrompterPlayerScreen(
    state: PlaybackUiState,
    onExit: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPause: () -> Unit,
    onReachedEnd: () -> Unit,
    onUpdateProgress: (Float) -> Unit,
    onSetHudVisible: (Boolean) -> Unit,
    onToggleAppearanceSheet: () -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onNudgeSpeed: (Float) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    onMarginChanged: (Float) -> Unit,
    onAlignmentChanged: (Int) -> Unit,
    onCountdownChanged: (Int) -> Unit,
    onGuideOpacityChanged: (Float) -> Unit,
    onToggleMirrorH: () -> Unit,
    onToggleMirrorV: () -> Unit,
    onAdjacentSong: (Int) -> Unit,
    onShowTransientBadge: (String) -> Unit,
    registerHardwareKeyHandler: (((AndroidKeyEvent) -> Boolean)?) -> Unit
) {
    BackHandler(onBack = onExit)

    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Keep screen awake & enable immersive stage mode while player is open
    DisposableEffect(context) {
        val activity = context.findActivity()
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val insetsController = window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Core high-precision scroll geometry
    var scrollOffsetPx by remember(state.scriptId) { mutableFloatStateOf(0f) }
    var viewportHeightPx by remember { mutableFloatStateOf(1f) }
    var contentHeightPx by remember { mutableFloatStateOf(1f) }
    var textLayoutResult by remember(state.scriptId) { mutableStateOf<TextLayoutResult?>(null) }
    var hudInteractionTick by remember { mutableIntStateOf(0) }
    val jumpAnimatable = remember(state.scriptId) { Animatable(0f) }

    val maxScrollPx = (contentHeightPx - viewportHeightPx).coerceAtLeast(0f)

    val highlightedText = remember(state.content) {
        ScriptTagHighlighter.highlight(state.content)
    }
    val sectionCharOffsets = remember(state.content) {
        ScriptTagHighlighter.findSectionCharacterOffsets(state.content)
    }

    // Keep latest references for hardware pedal callback and frame loop
    val latestState by rememberUpdatedState(state)
    val latestMaxScroll by rememberUpdatedState(maxScrollPx)
    val latestViewportHeight by rememberUpdatedState(viewportHeightPx)
    val latestTextLayout by rememberUpdatedState(textLayoutResult)

    fun smoothJumpTo(targetPx: Float) {
        val clamped = targetPx.coerceIn(0f, latestMaxScroll)
        coroutineScope.launch {
            jumpAnimatable.snapTo(scrollOffsetPx)
            jumpAnimatable.animateTo(
                targetValue = clamped,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) {
                scrollOffsetPx = value
                if (latestMaxScroll > 0f) {
                    onUpdateProgress(value / latestMaxScroll)
                }
            }
        }
    }

    fun jumpSection(direction: Int) {
        val topSpacerPx = latestViewportHeight * 0.45f
        val layout = latestTextLayout
        val sectionOffsetsPx = if (layout != null && sectionCharOffsets.isNotEmpty()) {
            sectionCharOffsets.mapNotNull { charIdx ->
                runCatching {
                    val safeIdx = charIdx.coerceIn(0, (latestState.content.length - 1).coerceAtLeast(0))
                    val line = layout.getLineForOffset(safeIdx)
                    // Offset inside Column where text begins after topSpacerPx
                    val lineTop = topSpacerPx + layout.getLineTop(line)
                    // Align line right on the center Reading Guide (viewportHeight * 0.45f)
                    (lineTop - topSpacerPx).coerceIn(0f, latestMaxScroll)
                }.getOrNull()
            }.distinct().sorted()
        } else {
            emptyList()
        }

        val current = scrollOffsetPx
        val stepFallback = latestViewportHeight * 0.35f
        val target = if (direction > 0) {
            sectionOffsetsPx.firstOrNull { it > current + 28f }
                ?: (current + stepFallback).coerceAtMost(latestMaxScroll)
        } else {
            sectionOffsetsPx.lastOrNull { it < current - 28f }
                ?: (current - stepFallback).coerceAtLeast(0f)
        }

        smoothJumpTo(target)
        onShowTransientBadge(if (direction > 0) "SECCIÓN SIGUIENTE ⏭" else "⏮ SECCIÓN ANTERIOR")
    }

    fun resetToTop() {
        onPause()
        smoothJumpTo(0f)
        onShowTransientBadge("INICIO 0%")
    }

    // Unified Hardware Keyboard & Bluetooth Pedal Handler
    val handleNativeKeyEvent: (AndroidKeyEvent) -> Boolean = remember {
        { event: AndroidKeyEvent ->
            if (event.action != AndroidKeyEvent.ACTION_DOWN) {
                false
            } else {
                when (event.keyCode) {
                    AndroidKeyEvent.KEYCODE_SPACE,
                    AndroidKeyEvent.KEYCODE_ENTER,
                    AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (event.repeatCount == 0) {
                            onTogglePlayPause()
                        }
                        true
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_UP,
                    AndroidKeyEvent.KEYCODE_VOLUME_UP -> {
                        val step = if (latestState.scrollSpeed >= 15f) 1.0f else 0.5f
                        onNudgeSpeed(step)
                        hudInteractionTick++
                        true
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN,
                    AndroidKeyEvent.KEYCODE_VOLUME_DOWN -> {
                        val step = if (latestState.scrollSpeed > 15f) 1.0f else 0.5f
                        onNudgeSpeed(-step)
                        hudInteractionTick++
                        true
                    }
                    AndroidKeyEvent.KEYCODE_PAGE_DOWN,
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                        jumpSection(+1)
                        true
                    }
                    AndroidKeyEvent.KEYCODE_PAGE_UP,
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                        jumpSection(-1)
                        true
                    }
                    AndroidKeyEvent.KEYCODE_R,
                    AndroidKeyEvent.KEYCODE_MOVE_HOME -> {
                        resetToTop()
                        true
                    }
                    AndroidKeyEvent.KEYCODE_ESCAPE -> {
                        onExit()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    // Register global Activity key interceptor so Bluetooth pedals work regardless of focus
    DisposableEffect(handleNativeKeyEvent) {
        registerHardwareKeyHandler(handleNativeKeyEvent)
        onDispose { registerHardwareKeyHandler(null) }
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    // Ultra-Smooth 60/120 FPS Scroll Engine using withFrameNanos
    LaunchedEffect(state.isPlaying, state.countdownRemaining, state.scriptId) {
        if (!state.isPlaying || state.countdownRemaining > 0) return@LaunchedEffect

        var lastFrameNanos = 0L
        var progressEmitAccumulator = 0f

        while (latestState.isPlaying) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    val deltaSec = ((frameNanos - lastFrameNanos) / 1_000_000_000f)
                        .coerceIn(0f, 0.05f)
                    // Calibrated speed curve: 0.5 -> ~9dp/s, 8.0 -> ~80dp/s, 50.0 -> ~500dp/s
                    val speedDpPerSec = latestState.scrollSpeed * 10.5f
                    val speedPxPerSec = speedDpPerSec * density.density
                    val nextOffset = scrollOffsetPx + (speedPxPerSec * deltaSec)

                    if (latestMaxScroll > 0f && nextOffset >= latestMaxScroll) {
                        scrollOffsetPx = latestMaxScroll
                        onReachedEnd()
                    } else {
                        scrollOffsetPx = nextOffset.coerceAtLeast(0f)
                        progressEmitAccumulator += deltaSec
                        if (progressEmitAccumulator >= 0.12f && latestMaxScroll > 0f) {
                            progressEmitAccumulator = 0f
                            onUpdateProgress(scrollOffsetPx / latestMaxScroll)
                        }
                    }
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    // Auto-hide HUD 3.5 seconds after playback begins (unless Appearance Sheet is open)
    LaunchedEffect(state.isPlaying, state.isHudVisible, state.isAppearanceSheetOpen, hudInteractionTick) {
        if (state.isPlaying && state.isHudVisible && !state.isAppearanceSheetOpen) {
            delay(3_500L)
            onSetHudVisible(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StagePureBlack)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    handleNativeKeyEvent(keyEvent.nativeKeyEvent)
                } else {
                    false
                }
            }
            .testTag("prompter_player_root")
    ) {
        // 1. Sub-Pixel GPU Teleprompter Text Viewport
        val topSpacerDp = with(density) { (viewportHeightPx * 0.45f).toDp() }
        val bottomSpacerDp = with(density) { (viewportHeightPx * 0.55f).toDp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { size ->
                    viewportHeightPx = size.height.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(state.scriptId) {
                    detectTapGestures(
                        onTap = { onTogglePlayPause() }
                    )
                }
                .pointerInput(state.scriptId, state.mirrorVertical) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val effectiveDrag = if (state.mirrorVertical) -dragAmount else dragAmount
                        scrollOffsetPx = (scrollOffsetPx - effectiveDrag).coerceIn(0f, latestMaxScroll)
                        if (latestMaxScroll > 0f) {
                            onUpdateProgress(scrollOffsetPx / latestMaxScroll)
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = if (state.mirrorHorizontal) -1f else 1f
                    scaleY = if (state.mirrorVertical) -1f else 1f
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .onSizeChanged { size ->
                        contentHeightPx = size.height.toFloat().coerceAtLeast(1f)
                    }
                    .graphicsLayer {
                        translationY = -scrollOffsetPx
                    }
                    .padding(horizontal = state.horizontalMarginDp.dp)
            ) {
                Spacer(modifier = Modifier.height(topSpacerDp))

                Text(
                    text = highlightedText,
                    style = TextStyle(
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = state.fontSizeSp.sp,
                        lineHeight = (state.fontSizeSp * 1.38f).sp,
                        color = StageWhite,
                        textAlign = ScriptAlignment.toTextAlign(state.textAlignment)
                    ),
                    onTextLayout = { result -> textLayoutResult = result },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prompter_script_text")
                )

                Spacer(modifier = Modifier.height(bottomSpacerDp))
            }
        }

        // 2. Center Visual Reading Guide Band (opacity adjustable 0%..75%)
        if (state.readingGuideOpacity > 0.01f) {
            ReadingGuideOverlay(
                opacity = state.readingGuideOpacity,
                guideHeightDp = (state.fontSizeSp * 1.65f).coerceIn(64f, 150f),
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Subtle Floating "Controles" Pill when HUD is hidden during playback
        AnimatedVisibility(
            visible = !state.isHudVisible && state.countdownRemaining == 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(14.dp)
        ) {
            Surface(
                onClick = {
                    hudInteractionTick++
                    onSetHudVisible(true)
                },
                shape = RoundedCornerShape(20.dp),
                color = StageObsidian.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, StageBorder.copy(alpha = 0.6f)),
                modifier = Modifier.testTag("show_hud_pill_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = stringResource(R.string.show_controls),
                        tint = BroadcastAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "VEL ${PrompterViewModel.formatSpeed(state.scrollSpeed)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = StageTextPrimary
                    )
                }
            }
        }

        // 4. Live Transient Feedback Badge (Bluetooth Pedal Speed / Section Jump / Mirror)
        AnimatedVisibility(
            visible = state.liveFeedbackBadge != null && state.countdownRemaining == 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 74.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BroadcastAmberContainer.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, BroadcastAmber)
            ) {
                Text(
                    text = state.liveFeedbackBadge.orEmpty(),
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BroadcastAmber,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
        }

        // 5. Auto-Hiding Top HUD Bar
        AnimatedVisibility(
            visible = state.isHudVisible && state.countdownRemaining == 0,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = StageObsidian.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, StageBorder),
                shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier.testTag("player_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.exit_player),
                            tint = StageTextPrimary
                        )
                    }

                    IconButton(
                        onClick = { onAdjacentSong(-1) },
                        modifier = Modifier.testTag("player_prev_song_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = stringResource(R.string.prev_song),
                            tint = StageTextSecondary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = StageTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${(state.scrollProgress * 100).toInt()}% · ${stringResource(R.string.pedal_shortcuts_hint)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = StageTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onAdjacentSong(+1) },
                        modifier = Modifier.testTag("player_next_song_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = stringResource(R.string.next_song),
                            tint = StageTextSecondary
                        )
                    }

                    // Reset to start
                    IconButton(
                        onClick = {
                            hudInteractionTick++
                            resetToTop()
                        },
                        modifier = Modifier.testTag("player_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = stringResource(R.string.reset_scroll),
                            tint = StageTextPrimary
                        )
                    }

                    // Countdown cycle (0s -> 3s -> 5s)
                    val nextCountdown = when (state.countdownSeconds) {
                        0 -> 3
                        3 -> 5
                        else -> 0
                    }
                    HudTogglePill(
                        label = "${state.countdownSeconds}s",
                        icon = Icons.Default.Timer,
                        active = state.countdownSeconds > 0,
                        onClick = {
                            hudInteractionTick++
                            onCountdownChanged(nextCountdown)
                        },
                        modifier = Modifier.testTag("player_countdown_button")
                    )

                    // Mirror Horizontal
                    HudTogglePill(
                        label = "H",
                        icon = Icons.Default.Flip,
                        active = state.mirrorHorizontal,
                        onClick = {
                            hudInteractionTick++
                            onToggleMirrorH()
                        },
                        modifier = Modifier.testTag("player_mirror_h_button")
                    )

                    // Mirror Vertical
                    HudTogglePill(
                        label = "V",
                        icon = Icons.Default.Flip,
                        active = state.mirrorVertical,
                        rotateIcon90 = true,
                        onClick = {
                            hudInteractionTick++
                            onToggleMirrorV()
                        },
                        modifier = Modifier.testTag("player_mirror_v_button")
                    )

                    // Visual Settings Dock Toggle
                    HudTogglePill(
                        label = "Aa",
                        icon = Icons.Default.Tune,
                        active = state.isAppearanceSheetOpen,
                        onClick = {
                            hudInteractionTick++
                            onToggleAppearanceSheet()
                        },
                        modifier = Modifier.testTag("player_appearance_button")
                    )
                }
            }
        }

        // 6. Auto-Hiding Bottom Live Controls + Expandable Appearance Panel
        AnimatedVisibility(
            visible = state.isHudVisible && state.countdownRemaining == 0,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Expandable Live Font Size / Margins / Guide Opacity / Alignment Panel
                AnimatedVisibility(visible = state.isAppearanceSheetOpen) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = StageObsidian.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, StageBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HudSliderRow(
                                label = stringResource(R.string.font_size_label),
                                valueText = "${state.fontSizeSp.toInt()} sp",
                                value = state.fontSizeSp,
                                range = ScriptEntity.MIN_FONT_SIZE_SP..ScriptEntity.MAX_FONT_SIZE_SP,
                                accent = CueMusicCyan,
                                onValueChange = {
                                    hudInteractionTick++
                                    onFontSizeChanged(it)
                                },
                                testTag = "player_font_size_slider"
                            )

                            HudSliderRow(
                                label = stringResource(R.string.margins_label),
                                valueText = "${state.horizontalMarginDp.toInt()} dp",
                                value = state.horizontalMarginDp,
                                range = ScriptEntity.MIN_MARGIN_DP..ScriptEntity.MAX_MARGIN_DP,
                                accent = BroadcastAmber,
                                onValueChange = {
                                    hudInteractionTick++
                                    onMarginChanged(it)
                                },
                                testTag = "player_margins_slider"
                            )

                            HudSliderRow(
                                label = stringResource(R.string.guide_opacity_label),
                                valueText = "${(state.readingGuideOpacity * 100).toInt()}%",
                                value = state.readingGuideOpacity,
                                range = 0f..0.75f,
                                accent = BroadcastAmber,
                                onValueChange = {
                                    hudInteractionTick++
                                    onGuideOpacityChanged(it)
                                },
                                testTag = "player_guide_opacity_slider"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.alignment_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = StageTextSecondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    HudAlignChip(
                                        selected = state.textAlignment == ScriptAlignment.START,
                                        icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                                        label = stringResource(R.string.align_left),
                                        onClick = { onAlignmentChanged(ScriptAlignment.START) }
                                    )
                                    HudAlignChip(
                                        selected = state.textAlignment == ScriptAlignment.CENTER,
                                        icon = Icons.Default.FormatAlignCenter,
                                        label = stringResource(R.string.align_center),
                                        onClick = { onAlignmentChanged(ScriptAlignment.CENTER) }
                                    )
                                    HudAlignChip(
                                        selected = state.textAlignment == ScriptAlignment.END,
                                        icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                                        label = stringResource(R.string.align_right),
                                        onClick = { onAlignmentChanged(ScriptAlignment.END) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Speed + Giant Play/Pause Stage Dock
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = StageObsidian.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, StageBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Giant Play / Pause Button
                        Surface(
                            onClick = onTogglePlayPause,
                            shape = CircleShape,
                            color = if (state.isPlaying) StageSurface else BroadcastAmber,
                            border = BorderStroke(2.dp, BroadcastAmber),
                            modifier = Modifier
                                .size(62.dp)
                                .testTag("player_play_pause_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (state.isPlaying) {
                                        stringResource(R.string.pause_action)
                                    } else {
                                        stringResource(R.string.play_action)
                                    },
                                    tint = if (state.isPlaying) BroadcastAmber else OnBroadcastAmber,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        // Minus Speed Button
                        Surface(
                            onClick = {
                                hudInteractionTick++
                                onNudgeSpeed(-0.5f)
                            },
                            shape = CircleShape,
                            color = StageSurface,
                            border = BorderStroke(1.dp, StageBorder),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("player_speed_minus_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Reducir velocidad",
                                    tint = StageTextPrimary
                                )
                            }
                        }

                        // Live Speed Slider (0.5 .. 50.0)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.speed_label).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StageTextSecondary
                                )
                                Text(
                                    text = PrompterViewModel.formatSpeed(state.scrollSpeed),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = BroadcastAmber,
                                    modifier = Modifier.testTag("player_speed_value")
                                )
                            }
                            Slider(
                                value = state.scrollSpeed,
                                onValueChange = {
                                    hudInteractionTick++
                                    onSpeedChanged(it)
                                },
                                valueRange = ScriptEntity.MIN_SCROLL_SPEED..ScriptEntity.MAX_SCROLL_SPEED,
                                colors = SliderDefaults.colors(
                                    thumbColor = BroadcastAmber,
                                    activeTrackColor = BroadcastAmber,
                                    inactiveTrackColor = StageBorder
                                ),
                                modifier = Modifier.testTag("player_speed_slider")
                            )
                        }

                        // Plus Speed Button
                        Surface(
                            onClick = {
                                hudInteractionTick++
                                onNudgeSpeed(+0.5f)
                            },
                            shape = CircleShape,
                            color = StageSurface,
                            border = BorderStroke(1.dp, StageBorder),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("player_speed_plus_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Aumentar velocidad",
                                    tint = StageTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Full-Screen Countdown Overlay (3, 2, 1)
        AnimatedVisibility(
            visible = state.countdownRemaining > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StagePureBlack.copy(alpha = 0.72f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onTogglePlayPause() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = StageObsidian,
                    border = BorderStroke(4.dp, BroadcastAmber),
                    modifier = Modifier.size(180.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = state.countdownRemaining.toString(),
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 84.sp,
                            color = BroadcastAmber,
                            modifier = Modifier.testTag("player_countdown_number")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingGuideOverlay(
    opacity: Float,
    guideHeightDp: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val bandHeightPx = guideHeightDp.dp.toPx()
        val centerY = size.height * 0.48f
        val topY = centerY - (bandHeightPx / 2f)

        // Subtle horizontal translucent reading band
        drawRect(
            color = BroadcastAmber.copy(alpha = opacity * 0.24f),
            topLeft = Offset(0f, topY),
            size = androidx.compose.ui.geometry.Size(size.width, bandHeightPx)
        )

        // Hairline top & bottom boundaries
        val lineColor = BroadcastAmber.copy(alpha = (opacity * 1.15f).coerceAtMost(0.9f))
        drawLine(
            color = lineColor,
            start = Offset(0f, topY),
            end = Offset(size.width, topY),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = lineColor,
            start = Offset(0f, topY + bandHeightPx),
            end = Offset(size.width, topY + bandHeightPx),
            strokeWidth = 1.5.dp.toPx()
        )

        // Crisp left & right stage eye-line triangles (▶ ... ◀)
        val triWidth = 14.dp.toPx()
        val triHalfHeight = 11.dp.toPx()
        val leftTriangle = Path().apply {
            moveTo(6.dp.toPx(), centerY - triHalfHeight)
            lineTo(6.dp.toPx() + triWidth, centerY)
            lineTo(6.dp.toPx(), centerY + triHalfHeight)
            close()
        }
        val rightTriangle = Path().apply {
            moveTo(size.width - 6.dp.toPx(), centerY - triHalfHeight)
            lineTo(size.width - 6.dp.toPx() - triWidth, centerY)
            lineTo(size.width - 6.dp.toPx(), centerY + triHalfHeight)
            close()
        }
        val markerColor = BroadcastAmber.copy(alpha = (opacity * 1.6f).coerceIn(0.25f, 1f))
        drawPath(leftTriangle, color = markerColor)
        drawPath(rightTriangle, color = markerColor)
    }
}

@Composable
private fun HudTogglePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    rotateIcon90: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (active) BroadcastAmberContainer else StageSurface,
        border = BorderStroke(1.dp, if (active) BroadcastAmber else StageBorder),
        modifier = modifier.height(42.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) BroadcastAmber else StageTextSecondary,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = if (rotateIcon90) 90f else 0f }
            )
            Text(
                text = label,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (active) BroadcastAmber else StageTextPrimary
            )
        }
    }
}

@Composable
private fun HudSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = StageTextSecondary,
            modifier = Modifier.width(120.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = StageBorder
            ),
            modifier = Modifier
                .weight(1f)
                .testTag(testTag)
        )
        Text(
            text = valueText,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = accent,
            modifier = Modifier.width(56.dp)
        )
    }
}

@Composable
private fun HudAlignChip(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) BroadcastAmberContainer else StageSurface,
        border = BorderStroke(1.dp, if (selected) BroadcastAmber else StageBorder),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) BroadcastAmber else StageTextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) BroadcastAmber else StageTextPrimary
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
