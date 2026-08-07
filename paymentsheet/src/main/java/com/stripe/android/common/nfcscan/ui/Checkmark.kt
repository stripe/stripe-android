package com.stripe.android.common.nfcscan.ui

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.PrimaryButtonTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun Checkmark(
    completionEventDelay: Duration,
    onAnimationComplete: () -> Unit,
    modifier: Modifier,
) {
    val onAnimationComplete by rememberUpdatedState(onAnimationComplete)
    val isInspectionMode = LocalInspectionMode.current

    if (isInspectionMode) {
        LaunchedEffect(Unit) {
            onAnimationComplete()
        }

        CheckmarkCanvas(
            progress = CHECKMARK_STATIC_PROGRESS,
            modifier = modifier.checkmarkSemantics(progress = CHECKMARK_STATIC_PROGRESS),
        )

        return
    }

    var savedState by rememberSaveable(stateSaver = CheckmarkSavedStateSaver) {
        mutableStateOf(CheckmarkSavedState())
    }

    val progress = remember { Animatable(savedState.progress) }

    LaunchedEffect(Unit) {
        runCheckmarkEffect(
            progress = progress,
            savedState = savedState,
            completionEventDelay = completionEventDelay,
            onSavedStateChange = { savedState = it },
            onAnimationComplete = { onAnimationComplete() },
        )
    }

    CheckmarkCanvas(
        progress = progress.value,
        modifier = modifier.checkmarkSemantics(progress = progress.value),
    )
}

@Composable
private fun CheckmarkCanvas(
    progress: Float,
    modifier: Modifier,
) {
    val color = PrimaryButtonTheme.colors.onBackground

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val canvasWidth = size.width

            val full = Path().apply {
                moveTo(canvasWidth * CHECKMARK_START_X_FRACTION, canvasWidth * CHECKMARK_START_Y_FRACTION)
                lineTo(canvasWidth * CHECKMARK_MIDDLE_X_FRACTION, canvasWidth * CHECKMARK_MIDDLE_Y_FRACTION)
                lineTo(canvasWidth * CHECKMARK_END_X_FRACTION, canvasWidth * CHECKMARK_END_Y_FRACTION)
            }

            val measure = PathMeasure().apply { setPath(full, false) }
            val drawn = Path()

            measure.getSegment(
                startDistance = 0f,
                stopDistance = measure.length * progress.coerceIn(0f, 1f),
                destination = drawn,
                startWithMoveTo = true,
            )

            drawPath(
                path = drawn,
                color = color,
                style = Stroke(
                    width = canvasWidth * CHECKMARK_STROKE_WIDTH_FRACTION,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

private fun Modifier.checkmarkSemantics(progress: Float): Modifier {
    return testTag(CHECKMARK_TEST_TAG)
        .semantics {
            checkmarkProgress = CheckmarkProgress(progress = progress)
        }
}

private suspend fun runCheckmarkEffect(
    progress: Animatable<Float, *>,
    savedState: CheckmarkSavedState,
    completionEventDelay: Duration,
    onSavedStateChange: (CheckmarkSavedState) -> Unit,
    onAnimationComplete: () -> Unit,
) {
    if (savedState.completionEventFired) {
        progress.snapTo(1f)
        return
    }

    progress.snapTo(savedState.progress)

    var currentSavedState = savedState

    coroutineScope {
        val progressSaver = launch {
            snapshotFlow { progress.value }
                .collect { value ->
                    currentSavedState = currentSavedState.withProgress(value)
                    onSavedStateChange(currentSavedState)
                }
        }

        try {
            if (!currentSavedState.animationComplete) {
                if (progress.value < 1f) {
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = CHECKMARK_DRAW_DURATION_MS,
                            easing = CheckmarkDrawEasing,
                        ),
                    )
                }
                currentSavedState = currentSavedState.markedAnimationComplete()
                onSavedStateChange(currentSavedState)
            }
        } finally {
            progressSaver.cancel()
        }
    }

    delayRemainingSuccessTime(
        delayDuration = completionEventDelay.inWholeMilliseconds,
        successDelayStartMs = currentSavedState.completionDelayStartMs,
    )
    onSavedStateChange(currentSavedState.copy(completionEventFired = true))
    onAnimationComplete()
}

private suspend fun delayRemainingSuccessTime(
    delayDuration: Long,
    successDelayStartMs: Long,
) {
    if (successDelayStartMs < 0L) {
        return
    }

    val remainingMs = delayDuration -
        (SystemClock.elapsedRealtime() - successDelayStartMs)

    if (remainingMs > 0L) {
        delay(remainingMs.milliseconds)
    }
}

internal data class CheckmarkProgress(
    val progress: Float = 0f,
)

private data class CheckmarkSavedState(
    val progress: Float = 0f,
    val animationComplete: Boolean = false,
    val completionEventFired: Boolean = false,
    val completionDelayStartMs: Long = UNSET_COMPLETION_DELAY_START_MS,
) {
    fun withProgress(progress: Float): CheckmarkSavedState {
        return copy(progress = progress)
    }

    fun markedAnimationComplete(
        nowMs: Long = SystemClock.elapsedRealtime(),
    ): CheckmarkSavedState {
        return copy(
            progress = 1f,
            animationComplete = true,
            completionDelayStartMs = completionDelayStartMs.takeIf { it >= 0L }
                ?: nowMs,
        )
    }
}

private val CheckmarkSavedStateSaver = listSaver(
    save = { state ->
        listOf(
            state.progress,
            state.animationComplete,
            state.completionEventFired,
            state.completionDelayStartMs,
        )
    },
    restore = { saved ->
        CheckmarkSavedState(
            progress = saved[0] as? Float ?: 0f,
            animationComplete = saved[1] as? Boolean ?: false,
            completionEventFired = saved[2] as? Boolean ?: false,
            completionDelayStartMs = saved[3] as? Long ?: UNSET_COMPLETION_DELAY_START_MS,
        )
    },
)

private const val UNSET_COMPLETION_DELAY_START_MS = -1L

internal const val CHECKMARK_TEST_TAG = "nfc_checkmark"
internal const val CHECKMARK_DRAW_DURATION_MS = 450

internal val CheckmarkProgressKey = SemanticsPropertyKey<CheckmarkProgress>(
    name = "CheckmarkProgress",
    mergePolicy = { _, new -> new },
)

internal var SemanticsPropertyReceiver.checkmarkProgress by CheckmarkProgressKey

private const val CHECKMARK_START_X_FRACTION = 0.2f
private const val CHECKMARK_START_Y_FRACTION = 0.5f
private const val CHECKMARK_MIDDLE_X_FRACTION = 0.4f
private const val CHECKMARK_MIDDLE_Y_FRACTION = 0.72f
private const val CHECKMARK_END_X_FRACTION = 0.8f
private const val CHECKMARK_END_Y_FRACTION = 0.28f
private const val CHECKMARK_STROKE_WIDTH_FRACTION = 5f / 48f
private const val CHECKMARK_STATIC_PROGRESS = 1f

private val CheckmarkDrawEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
