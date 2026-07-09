package com.stripe.android.common.nfcscan.ui

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val ARC_COMPLETE_DURATION_MS = 270
private const val CHECKMARK_ALPHA_DURATION_MS = 144
private const val CHECKMARK_DRAW_DELAY_MS = 48
private const val CHECKMARK_DRAW_DURATION_MS = 168

private val SuccessDelay = 0.9.seconds
private val CoilIconSize = 96.dp

private val CheckmarkDrawEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

@Composable
internal fun NfcCoilAnimatedInterior(
    status: NfcScanningStatus,
    onSuccessShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ringState = rememberNfcCoilRingState(status, onSuccessShown)

    Box(
        modifier = modifier
            .testTag(NFC_COIL_ANIMATED_INTERIOR_TEST_TAG)
            .semantics { nfcCoilRingProgress = ringState },
        contentAlignment = Alignment.Center,
    ) {
        NfcCoilSuccessRing(
            arcProgress = ringState.arcProgress,
            checkmarkProgress = ringState.checkmarkProgress,
            checkmarkAlpha = ringState.checkmarkAlpha,
            modifier = Modifier.fillMaxSize(),
        )

        NfcCoilContactlessIcon(
            status = status,
            modifier = Modifier
                .size(CoilIconSize)
                .graphicsLayer {
                    alpha = 1f - ringState.checkmarkAlpha
                },
        )
    }
}

@Composable
private fun rememberNfcCoilRingState(
    status: NfcScanningStatus,
    onSuccessShown: () -> Unit,
): NfcCoilRingProgress {
    if (LocalInspectionMode.current) {
        return when (status) {
            NfcScanningStatus.Scanned -> NfcCoilRingProgress.Complete
            NfcScanningStatus.Idle,
            NfcScanningStatus.Scanning -> NfcCoilRingProgress.Zero
        }
    }

    var savedState by rememberSaveable(stateSaver = NfcCoilRingSavedStateSaver) {
        mutableStateOf(NfcCoilRingSavedState())
    }

    val animatables = rememberRingAnimatables(savedState.progress)
    val onSuccessShown by rememberUpdatedState(onSuccessShown)

    LaunchedEffect(status) {
        when (status) {
            NfcScanningStatus.Idle,
            NfcScanningStatus.Scanning -> {
                animatables.snapTo(NfcCoilRingProgress.Zero)
                savedState = NfcCoilRingSavedState()
            }
            NfcScanningStatus.Scanned -> {
                runScannedRingEffect(
                    animatables = animatables,
                    savedState = savedState,
                    onSavedStateChange = { savedState = it },
                    onSuccessShown = onSuccessShown,
                )
            }
        }
    }

    return animatables.progress
}

@Composable
private fun rememberRingAnimatables(initialProgress: NfcCoilRingProgress): RingAnimatables {
    return remember {
        RingAnimatables(
            arcProgress = Animatable(initialProgress.arcProgress),
            checkmarkProgress = Animatable(initialProgress.checkmarkProgress),
            checkmarkAlpha = Animatable(initialProgress.checkmarkAlpha),
        )
    }
}

private suspend fun runScannedRingEffect(
    animatables: RingAnimatables,
    savedState: NfcCoilRingSavedState,
    onSavedStateChange: (NfcCoilRingSavedState) -> Unit,
    onSuccessShown: () -> Unit,
) {
    animatables.snapTo(savedState.progress)

    if (savedState.successShown) {
        animatables.snapTo(NfcCoilRingProgress.Complete)
        return
    }

    var currentSavedState = savedState
    coroutineScope {
        val progressSaver = launch {
            snapshotFlow { animatables.progress }
                .collect { progress ->
                    currentSavedState = currentSavedState.withProgress(progress)
                    onSavedStateChange(currentSavedState)
                }
        }

        try {
            if (!currentSavedState.animationComplete) {
                playSuccessRingAnimation(animatables)
                currentSavedState = currentSavedState.markedAnimationComplete()
                onSavedStateChange(currentSavedState)
            }
        } finally {
            progressSaver.cancel()
        }
    }

    delayRemainingSuccessTime(currentSavedState.successDelayStartMs)
    onSavedStateChange(currentSavedState.copy(successShown = true))
    onSuccessShown()
}

private suspend fun delayRemainingSuccessTime(successDelayStartMs: Long) {
    if (successDelayStartMs <= 0L) {
        return
    }

    val remainingMs = SuccessDelay.inWholeMilliseconds -
        (SystemClock.elapsedRealtime() - successDelayStartMs)
    if (remainingMs > 0L) {
        delay(remainingMs.milliseconds)
    }
}

private suspend fun playSuccessRingAnimation(animatables: RingAnimatables) {
    if (animatables.arcProgress.value < 1f) {
        animatables.arcProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = ARC_COMPLETE_DURATION_MS,
                easing = LinearEasing,
            ),
        )
    }
    if (animatables.checkmarkAlpha.value < 1f) {
        animatables.checkmarkAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = CHECKMARK_ALPHA_DURATION_MS),
        )
    }
    if (animatables.checkmarkProgress.value < 1f) {
        val drawDelayMillis = if (animatables.checkmarkProgress.value <= 0f) {
            CHECKMARK_DRAW_DELAY_MS
        } else {
            0
        }
        animatables.checkmarkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = CHECKMARK_DRAW_DURATION_MS,
                delayMillis = drawDelayMillis,
                easing = CheckmarkDrawEasing,
            ),
        )
    }
}

private class RingAnimatables(
    val arcProgress: Animatable<Float, *>,
    val checkmarkProgress: Animatable<Float, *>,
    val checkmarkAlpha: Animatable<Float, *>,
) {
    val progress: NfcCoilRingProgress
        get() = NfcCoilRingProgress(
            arcProgress = arcProgress.value,
            checkmarkProgress = checkmarkProgress.value,
            checkmarkAlpha = checkmarkAlpha.value,
        )

    suspend fun snapTo(progress: NfcCoilRingProgress) {
        arcProgress.snapTo(progress.arcProgress)
        checkmarkProgress.snapTo(progress.checkmarkProgress)
        checkmarkAlpha.snapTo(progress.checkmarkAlpha)
    }
}

private data class NfcCoilRingSavedState(
    val progress: NfcCoilRingProgress = NfcCoilRingProgress.Zero,
    val animationComplete: Boolean = false,
    val successShown: Boolean = false,
    val successDelayStartMs: Long = 0L,
) {
    fun withProgress(progress: NfcCoilRingProgress): NfcCoilRingSavedState {
        return copy(progress = progress)
    }

    fun markedAnimationComplete(
        nowMs: Long = SystemClock.elapsedRealtime(),
    ): NfcCoilRingSavedState {
        return copy(
            progress = NfcCoilRingProgress.Complete,
            animationComplete = true,
            successDelayStartMs = successDelayStartMs.takeIf { it > 0L } ?: nowMs,
        )
    }
}

private val NfcCoilRingSavedStateSaver = listSaver(
    save = { state ->
        listOf(
            state.progress.arcProgress,
            state.progress.checkmarkProgress,
            state.progress.checkmarkAlpha,
            state.animationComplete,
            state.successShown,
            state.successDelayStartMs,
        )
    },
    restore = { saved ->
        NfcCoilRingSavedState(
            progress = NfcCoilRingProgress(
                arcProgress = saved[0] as? Float ?: 0f,
                checkmarkProgress = saved[1] as? Float ?: 0f,
                checkmarkAlpha = saved[2] as? Float ?: 0f,
            ),
            animationComplete = saved[3] as? Boolean ?: false,
            successShown = saved[4] as? Boolean ?: false,
            successDelayStartMs = saved[5] as? Long ?: 0L,
        )
    },
)

internal const val NFC_COIL_ANIMATED_INTERIOR_TEST_TAG = "nfc_coil_animated_interior"

internal val NfcCoilRingProgressKey = SemanticsPropertyKey<NfcCoilRingProgress>(
    name = "NfcCoilRingProgress",
    mergePolicy = { _, new -> new },
)

internal var SemanticsPropertyReceiver.nfcCoilRingProgress by NfcCoilRingProgressKey

internal data class NfcCoilRingProgress(
    val arcProgress: Float = 0f,
    val checkmarkProgress: Float = 0f,
    val checkmarkAlpha: Float = 0f,
) {
    companion object {
        val Zero = NfcCoilRingProgress()
        val Complete = NfcCoilRingProgress(
            arcProgress = 1f,
            checkmarkProgress = 1f,
            checkmarkAlpha = 1f,
        )
    }
}
