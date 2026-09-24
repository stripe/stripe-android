package com.stripe.android.common.nfcscan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.common.nfcscan.NfcScanningViewAction
import com.stripe.android.common.nfcscan.NfcScanningViewState
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.paymentsheet.R

@Composable
internal fun NfcScanningScreen(
    state: NfcScanningViewState,
    viewActionHandler: (NfcScanningViewAction) -> Unit,
) {
    when (state) {
        NfcScanningViewState.NotSecure -> {
            DeveloperOptionsScreen(
                onOpenSettings = {
                    viewActionHandler(NfcScanningViewAction.OpenDeveloperOptions)
                },
            )
        }
        is NfcScanningViewState.Ready -> {
            val deviceRotation = rememberDeviceRotation()

            NfcScanningLayout(
                status = state.status,
                tapZone = state.tapZone,
                deviceRotation = deviceRotation,
                onClose = {
                    viewActionHandler(NfcScanningViewAction.Close)
                },
                onSuccessShown = {
                    viewActionHandler(NfcScanningViewAction.SuccessShown)
                },
                onErrorShown = {
                    viewActionHandler(NfcScanningViewAction.ErrorShown)
                },
            )
        }
    }
}

@Composable
internal fun DeveloperOptionsScreen(
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
            .windowInsetsPadding(WindowInsets.safeContent)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(164.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.stripe_ic_sail_warning_circle),
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colors.onSurface,
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.stripe_nfc_developer_options_title),
            color = MaterialTheme.colors.onSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.stripe_nfc_developer_options_message),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Normal),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onOpenSettings,
            modifier = Modifier.testTag(NFC_OPEN_DEVELOPER_OPTIONS_TEST_TAG).size(width = 180.dp, height = 46.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = stringResource(R.string.stripe_nfc_developer_options_open_settings),
                color = Color.White,
                fontSize = 16.sp,
                letterSpacing = 0.sp,
            )
        }
    }
}

@Composable
internal fun NfcScanningLayout(
    status: NfcScanningStatus,
    tapZone: TapZone,
    deviceRotation: DeviceRotation,
    onClose: () -> Unit,
    onSuccessShown: () -> Unit,
    onErrorShown: () -> Unit,
) {
    val tapZone = remember(deviceRotation, tapZone) {
        createNormalizedTapZone(deviceRotation, tapZone)
    }

    val canShowCloseButton = status !is NfcScanningStatus.Scanned

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface),
    ) {
        NfcCoilLayout(
            status = status,
            tapZone = tapZone,
            deviceRotation = deviceRotation,
            onSuccessShown = onSuccessShown,
            onErrorShown = onErrorShown,
        )
        CloseButtonLayout(canShowCloseButton, tapZone, deviceRotation, onClose)
    }
}

@Composable
private fun BoxScope.CloseButtonLayout(
    canShow: Boolean,
    tapZone: TapZone,
    deviceRotation: DeviceRotation,
    onClose: () -> Unit,
) {
    val (alignment, padding) = rememberOrientationValues(
        deviceRotation = deviceRotation,
        onPortrait = {
            if (tapZone.yBias > 0.1) {
                Alignment.TopEnd to PaddingValues(end = DefaultEdgePadding, top = DefaultEdgePadding)
            } else {
                Alignment.BottomCenter to PaddingValues(bottom = BottomCenterEdgePadding)
            }
        },
        onLandscape = {
            if (tapZone.xBias < 0.25) {
                Alignment.TopEnd to PaddingValues(end = DefaultEdgePadding, top = DefaultEdgePadding)
            } else {
                Alignment.TopStart to PaddingValues(start = DefaultEdgePadding, top = DefaultEdgePadding)
            }
        }
    )

    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeContent)
            .padding(padding)
            .align(alignment)
    ) {
        AnimatedVisibility(
            visible = canShow,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            NfcCloseButton(onClose)
        }
    }
}

private val DefaultEdgePadding = 20.dp
private val BottomCenterEdgePadding = 68.dp

internal const val NFC_OPEN_DEVELOPER_OPTIONS_TEST_TAG = "nfc_open_developer_options"
