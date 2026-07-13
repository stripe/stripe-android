package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.strings.resolve
import com.stripe.payments.model.R as PaymentsModelR

@Composable
internal fun NfcScanningFullScreenErrorLayout(
    message: ResolvableString,
    deviceRotation: DeviceRotation,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(PaymentsModelR.drawable.stripe_ic_error),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    color = MaterialTheme.colors.error,
                ),
                modifier = Modifier.size(36.dp),
            )

            Spacer(Modifier.size(18.dp))

            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = message.resolve(),
                color = MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.h4,
                textAlign = TextAlign.Center,
            )
        }

        ErrorCloseButtonLayout(deviceRotation, onClose)
    }
}

@Composable
private fun BoxScope.ErrorCloseButtonLayout(
    deviceRotation: DeviceRotation,
    onClose: () -> Unit,
) {
    val (alignment, padding) = rememberOrientationValues(
        deviceRotation = deviceRotation,
        onPortrait = {
            Alignment.BottomCenter to PaddingValues(bottom = BottomCenterEdgePadding)
        },
        onLandscape = {
            Alignment.TopStart to PaddingValues(start = DefaultEdgePadding, top = DefaultEdgePadding)
        },
    )

    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeContent)
            .padding(padding)
            .align(alignment),
    ) {
        NfcCloseButton(onClose)
    }
}

private val DefaultEdgePadding = 20.dp
private val BottomCenterEdgePadding = 68.dp
