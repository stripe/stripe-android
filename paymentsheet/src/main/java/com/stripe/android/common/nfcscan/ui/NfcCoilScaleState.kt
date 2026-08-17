package com.stripe.android.common.nfcscan.ui

import android.os.Parcelable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class NfcCoilScaleState(
    val coilScale: Float,
    val isComplete: Boolean,
) : Parcelable

@Composable
internal fun rememberNfcCoilScaleState(): State<NfcCoilScaleState> {
    if (LocalInspectionMode.current) {
        return remember {
            mutableStateOf(
                NfcCoilScaleState(
                    coilScale = MAX_PROGRESS,
                    isComplete = true,
                )
            )
        }
    }

    val savedState = rememberSaveable {
        mutableStateOf(
            NfcCoilScaleState(
                coilScale = COIL_SCALE_MIN_PROGRESS,
                isComplete = false,
            )
        )
    }

    val animationManager = remember { NfcCoilScaleManager(savedState) }

    LaunchedEffect(Unit) {
        animationManager.run()
    }

    return savedState
}

private class NfcCoilScaleManager(
    private val savedState: MutableState<NfcCoilScaleState>,
) {
    val coilScale = Animatable(savedState.value.coilScale)

    suspend fun run() = coroutineScope {
        if (savedState.value.isComplete) {
            return@coroutineScope
        }

        val saver = launch {
            snapshotFlow { coilScale.value }.collect {
                savedState.value = savedState.value.copy(coilScale = it)
            }
        }

        try {
            if (coilScale.value < MAX_PROGRESS) {
                coilScale.animateTo(
                    targetValue = MAX_PROGRESS,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }

            savedState.value = savedState.value.copy(
                coilScale = MAX_PROGRESS,
                isComplete = true,
            )
        } finally {
            saver.cancel()
        }
    }
}

private const val MAX_PROGRESS = 1f

private const val COIL_SCALE_MIN_PROGRESS = 0.4f
