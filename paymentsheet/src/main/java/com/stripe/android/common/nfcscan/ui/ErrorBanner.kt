package com.stripe.android.common.nfcscan.ui

import android.os.Parcelable
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.strings.resolve
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val ErrorShownDelay = 3.seconds
private val ErrorBannerShape = RoundedCornerShape(size = 12.dp)
private val ErrorBannerIconSize = 20.dp
private val ErrorBannerIconSpacing = 8.dp
private const val ErrorBannerBackgroundAlpha = 0.1f
private val ErrorBannerVerticalPadding = 11.dp

@Composable
internal fun ErrorBanner(
    error: ResolvableString?,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onShown by rememberUpdatedState(onShown)
    val isInspectionMode = LocalInspectionMode.current

    ErrorBannerContent(error, modifier)

    if (error == null) {
        return
    }

    if (isInspectionMode) {
        LaunchedEffect(Unit) {
            onShown()
        }

        return
    }

    val savedState = remember {
        mutableStateOf<ErrorBannerState>(ErrorBannerState.Init)
    }

    val delayManager = remember {
        ErrorBannerDelayManager(
            delay = ErrorShownDelay,
            savedState = savedState,
            onShown = onShown,
        )
    }

    LaunchedEffect(Unit) {
        delayManager.run()
    }
}

@Composable
private fun ErrorBannerContent(
    error: ResolvableString?,
    modifier: Modifier = Modifier,
) {
    val ref = remember {
        Ref<ResolvableString>()
    }

    ref.value = error ?: ref.value

    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn(
            spring(stiffness = Spring.StiffnessMedium),
        ),
        exit = fadeOut(
            spring(stiffness = Spring.StiffnessMedium),
        ),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .testTag(ERROR_BANNER_TEST_TAG)
                .background(
                    color = MaterialTheme.colors.error.copy(alpha = ErrorBannerBackgroundAlpha),
                    shape = ErrorBannerShape,
                )
                .padding(
                    top = ErrorBannerVerticalPadding,
                    bottom = ErrorBannerVerticalPadding,
                    start = 12.dp,
                    end = 16.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ref.value?.let { error ->
                Icon(
                    painter = painterResource(R.drawable.stripe_ic_nfc_error),
                    contentDescription = null,
                    modifier = Modifier.size(ErrorBannerIconSize),
                    tint = MaterialTheme.colors.error,
                )
                Text(
                    text = error.resolve(),
                    modifier = Modifier
                        .padding(start = ErrorBannerIconSpacing)
                        .weight(1f, fill = false),
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body1,
                )
            }
        }
    }
}

private class ErrorBannerDelayManager(
    private val delay: Duration,
    private val savedState: MutableState<ErrorBannerState>,
    private val onShown: () -> Unit,
) {
    suspend fun run() {
        if (getState() is ErrorBannerState.Complete) {
            return
        }

        if (getState() is ErrorBannerState.Init) {
            start()
        }

        val currentState = getState()

        if (currentState is ErrorBannerState.Waiting) {
            wait(currentState)
        }

        onShown()
    }

    private fun start() {
        savedState.value = ErrorBannerState.Waiting(SystemClock.elapsedRealtime())
    }

    private suspend fun wait(state: ErrorBannerState.Waiting) {
        val startedAt = state.startedAt

        if (startedAt < 0L) {
            return
        }

        val remainingMs = delay.inWholeMilliseconds - (SystemClock.elapsedRealtime() - startedAt)

        if (remainingMs > 0L) {
            delay(remainingMs.milliseconds)
        }

        savedState.value = ErrorBannerState.Complete
    }

    private fun getState() = savedState.value
}

private sealed interface ErrorBannerState : Parcelable {
    @Parcelize
    data object Init : ErrorBannerState

    @Parcelize
    data class Waiting(val startedAt: Long) : ErrorBannerState

    @Parcelize
    data object Complete : ErrorBannerState
}

internal const val ERROR_BANNER_TEST_TAG = "nfc_error_banner"
