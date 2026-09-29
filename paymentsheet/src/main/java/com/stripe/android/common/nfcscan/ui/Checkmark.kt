package com.stripe.android.common.nfcscan.ui

import android.os.Parcelable
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun Checkmark(
    startDelay: Duration,
    completionDelay: Duration,
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
            progress = CHECKMARK_MAX_PROGRESS,
            modifier = modifier,
        )

        return
    }

    val savedState = rememberSaveable {
        mutableStateOf<CheckmarkState>(CheckmarkState.Init)
    }

    val animationManager = remember {
        CheckmarkAnimationManager(
            startDelay = startDelay,
            completionDelay = completionDelay,
            savedState = savedState,
            onComplete = { onAnimationComplete() },
        )
    }

    LaunchedEffect(Unit) {
        animationManager.run()
    }

    CheckmarkCanvas(
        progress = animationManager.progress.value,
        modifier = modifier,
    )
}

@Composable
private fun CheckmarkCanvas(
    progress: Float,
    modifier: Modifier,
) {
    val color = PrimaryButtonTheme.colors.onBackground

    Box(modifier = modifier.checkmarkSemantics(progress = progress), contentAlignment = Alignment.Center) {
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
                stopDistance = measure.length * progress.coerceIn(CHECKMARK_MIN_PROGRESS, CHECKMARK_MAX_PROGRESS),
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

private class CheckmarkAnimationManager(
    private val startDelay: Duration,
    private val completionDelay: Duration,
    private val savedState: MutableState<CheckmarkState>,
    private val onComplete: () -> Unit,
) {
    val progress = when (val currentState = savedState.value) {
        is CheckmarkState.Init,
        is CheckmarkState.Starting -> Animatable(CHECKMARK_MIN_PROGRESS)
        is CheckmarkState.Progressing -> Animatable(
            initialValue = currentState.progress.coerceIn(CHECKMARK_MIN_PROGRESS, CHECKMARK_MAX_PROGRESS),
        )
        is CheckmarkState.Showing,
        is CheckmarkState.Complete -> Animatable(initialValue = CHECKMARK_MAX_PROGRESS)
    }

    suspend fun run() {
        if (getState() is CheckmarkState.Complete) {
            return
        }

        if (getState() is CheckmarkState.Init) {
            start()
        }

        val potentialStartingState = getState()

        if (potentialStartingState is CheckmarkState.Starting) {
            delay(
                delay = startDelay,
                startedAt = potentialStartingState.startedAt,
                transitionState = CheckmarkState.Progressing(CHECKMARK_MIN_PROGRESS)
            )
        }

        if (getState() is CheckmarkState.Progressing) {
            progress()
        }

        val potentialShowingState = getState()

        if (potentialShowingState is CheckmarkState.Showing) {
            delay(
                delay = completionDelay,
                startedAt = potentialShowingState.startedAt,
                transitionState = CheckmarkState.Complete,
            )
        }

        onComplete()
    }

    private fun start() {
        savedState.value = CheckmarkState.Starting(startedAt = SystemClock.elapsedRealtime())
    }

    private suspend fun progress() = coroutineScope {
        val progressSaver = launch {
            snapshotFlow { progress.value }.collect { value ->
                savedState.value = CheckmarkState.Progressing(value)
            }
        }

        try {
            if (progress.value < CHECKMARK_MAX_PROGRESS) {
                progress.animateTo(
                    targetValue = CHECKMARK_MAX_PROGRESS,
                    animationSpec = tween(
                        durationMillis = CHECKMARK_DRAW_DURATION_MS,
                        easing = CheckmarkDrawEasing,
                    ),
                )
            }

            savedState.value = CheckmarkState.Showing(startedAt = SystemClock.elapsedRealtime())
        } finally {
            progressSaver.cancel()
        }
    }

    private suspend fun delay(
        delay: Duration,
        startedAt: Long,
        transitionState: CheckmarkState,
    ) {
        if (startedAt < 0L) {
            return
        }

        val remainingMs = delay.inWholeMilliseconds - (SystemClock.elapsedRealtime() - startedAt)

        if (remainingMs > 0L) {
            delay(remainingMs.milliseconds)
        }

        savedState.value = transitionState
    }

    private fun getState() = savedState.value
}

private sealed interface CheckmarkState : Parcelable {
    @Parcelize
    data object Init : CheckmarkState

    @Parcelize
    data class Starting(val startedAt: Long) : CheckmarkState

    @Parcelize
    data class Progressing(val progress: Float) : CheckmarkState

    @Parcelize
    data class Showing(val startedAt: Long) : CheckmarkState

    @Parcelize
    data object Complete : CheckmarkState
}

private fun Modifier.checkmarkSemantics(progress: Float): Modifier {
    return testTag(CHECKMARK_TEST_TAG)
        .semantics {
            checkmarkProgress = progress
        }
}

internal const val CHECKMARK_TEST_TAG = "nfc_checkmark"
internal const val CHECKMARK_DRAW_DURATION_MS = 450

internal val CheckmarkProgressKey = SemanticsPropertyKey<Float>(
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

private const val CHECKMARK_MAX_PROGRESS = 1f
private const val CHECKMARK_MIN_PROGRESS = 0f

private val CheckmarkDrawEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
