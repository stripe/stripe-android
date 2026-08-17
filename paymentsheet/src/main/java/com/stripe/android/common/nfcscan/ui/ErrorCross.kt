package com.stripe.android.common.nfcscan.ui

import android.os.Parcelable
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.PrimaryButtonTheme
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun ErrorCross(
    shownDelay: Duration,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onShown by rememberUpdatedState(onShown)
    val isInspectionMode = LocalInspectionMode.current

    Box(
        modifier = modifier.testTag(ERROR_CROSS_TEST_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.stripe_ic_nfc_error_cross),
            contentDescription = null,
            tint = PrimaryButtonTheme.colors.onBackground,
            modifier = Modifier.size(ErrorCrossIconSize),
        )
    }

    if (isInspectionMode) {
        LaunchedEffect(Unit) {
            onShown()
        }

        return
    }

    val savedState = rememberSaveable {
        mutableStateOf<ErrorCrossState>(ErrorCrossState.Init)
    }

    val delayManager = remember {
        ErrorCrossDelayManager(
            delay = shownDelay,
            savedState = savedState,
            onShown = { onShown() },
        )
    }

    LaunchedEffect(Unit) {
        delayManager.run()
    }
}

private class ErrorCrossDelayManager(
    private val delay: Duration,
    private val savedState: MutableState<ErrorCrossState>,
    private val onShown: () -> Unit,
) {
    suspend fun run() {
        if (getState() is ErrorCrossState.Complete) {
            return
        }

        if (getState() is ErrorCrossState.Init) {
            start()
        }

        val currentState = getState()

        if (currentState is ErrorCrossState.Waiting) {
            wait(currentState)
        }

        onShown()
    }

    private fun start() {
        savedState.value = ErrorCrossState.Waiting(SystemClock.elapsedRealtime())
    }

    private suspend fun wait(state: ErrorCrossState.Waiting) {
        val startedAt = state.startedAt

        if (startedAt >= 0L) {
            val remainingMs = delay.inWholeMilliseconds - (SystemClock.elapsedRealtime() - startedAt)

            if (remainingMs > 0L) {
                delay(remainingMs.milliseconds)
            }
        }

        savedState.value = ErrorCrossState.Complete
    }

    private fun getState() = savedState.value
}

private sealed interface ErrorCrossState : Parcelable {
    @Parcelize
    data object Init : ErrorCrossState

    @Parcelize
    data class Waiting(val startedAt: Long) : ErrorCrossState

    @Parcelize
    data object Complete : ErrorCrossState
}

internal const val ERROR_CROSS_TEST_TAG = "nfc_error_cross"

private val ErrorCrossIconSize = 36.dp
