package com.stripe.android.common.nfcscan.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds

private val CheckmarkStartDelay = 0.15.seconds
private val CheckmarkCompletionDelay = 0.9.seconds
private val ErrorShownDelay = 1.7.seconds
private val CoilIconSize = 96.dp

@Composable
internal fun NfcCoilAnimatedInterior(
    status: NfcScanningStatus,
    onSuccessShown: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val checkmarkStateRetainer = rememberStateRetainer(
        key = CHECKMARK_STATE_KEY,
        canRetainState = status is NfcScanningStatus.Scanned,
    )

    val errorStateRetainer = rememberStateRetainer(
        key = ERROR_STATE_KEY,
        canRetainState = status is NfcScanningStatus.Error,
    )

    AnimatedContent(
        modifier = modifier,
        targetState = status,
        transitionSpec = {
            val fadeOut = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))

            if (targetState is NfcScanningStatus.Scanned) {
                EnterTransition.None togetherWith fadeOut
            } else {
                fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) togetherWith fadeOut
            }
        },
        label = "nfc_coil_animated_interior_content",
        contentAlignment = Alignment.Center,
    ) { status ->
        Box(
            modifier = Modifier.size(CoilIconSize),
            contentAlignment = Alignment.Center,
        ) {
            when (status) {
                is NfcScanningStatus.Idle -> {
                    NfcCoilContactlessIcon(Modifier.size(CoilIconSize))
                }
                NfcScanningStatus.Scanning -> Spinner()
                is NfcScanningStatus.Error -> errorStateRetainer.provider {
                    ErrorCross(
                        shownDelay = ErrorShownDelay,
                        onShown = onErrorShown,
                        modifier = Modifier.size(CoilIconSize)
                    )
                }
                is NfcScanningStatus.Scanned -> checkmarkStateRetainer.provider {
                    Checkmark(
                        startDelay = CheckmarkStartDelay,
                        completionDelay = CheckmarkCompletionDelay,
                        onAnimationComplete = onSuccessShown,
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}

private const val CHECKMARK_STATE_KEY = "CHECKMARK_STATE"
private const val ERROR_STATE_KEY = "ERROR_STATE"
