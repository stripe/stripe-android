package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds

private val SuccessDelay = 0.9.seconds
private val CoilIconSize = 96.dp

@Composable
internal fun NfcCoilAnimatedInterior(
    status: NfcScanningStatus,
    onSuccessShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            is NfcScanningStatus.Idle,
            NfcScanningStatus.Scanning -> {
                NfcCoilContactlessIcon(
                    status = status,
                    modifier = Modifier
                        .size(CoilIconSize)
                )
            }
            is NfcScanningStatus.Scanned -> {
                Checkmark(
                    completionEventDelay = SuccessDelay,
                    onAnimationComplete = onSuccessShown,
                    modifier = Modifier.size(CoilIconSize)
                )
            }
        }
    }
}
