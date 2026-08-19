package com.stripe.android.common.nfcscan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.paymentsheet.R
import kotlin.math.roundToInt

private val PortraitTopTextOffset = 40.5.dp
private val PortraitBottomTextOffset = 48.dp
private val LandscapeTopTextOffset = 22.5.dp
private val LandscapeBottomTextOffset = 30.dp
private val InstructionTextEdgePadding = 20.dp
private val ErrorTextTopSpacing = 20.dp

@Composable
internal fun NfcCoilTextLayout(
    containerWidth: Dp,
    containerHeight: Dp,
    tapZone: TapZone,
    coilSize: Dp,
    deviceRotation: DeviceRotation,
    shouldRenderTextAboveCoil: Boolean,
    canShow: Boolean,
    error: ErrorBannerParams?,
) {
    Layout(
        content = {
            NfcCoilInstructionText(canShow)
            ErrorBanner(error)
        },
    ) { measurables, constraints ->
        placeCoilTextElements(
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            tapZone = tapZone,
            coilSize = coilSize,
            deviceRotation = deviceRotation,
            shouldRenderTextAboveCoil = shouldRenderTextAboveCoil,
            measurables = measurables,
            constraints = constraints,
        )
    }
}

private fun MeasureScope.placeCoilTextElements(
    containerWidth: Dp,
    containerHeight: Dp,
    tapZone: TapZone,
    coilSize: Dp,
    deviceRotation: DeviceRotation,
    measurables: List<Measurable>,
    constraints: Constraints,
    shouldRenderTextAboveCoil: Boolean,
): MeasureResult {
    val horizontalBias = tapZone.xBias * 2 - 1
    val verticalBias = tapZone.yBias * 2 - 1

    val containerWidthPx = containerWidth.roundToPx()
    val containerHeightPx = containerHeight.roundToPx()

    if (measurables.isEmpty()) {
        return layout(containerWidthPx, containerHeightPx) {}
    }

    val coilSizePx = coilSize.roundToPx()
    val edgePaddingPx = InstructionTextEdgePadding.roundToPx()
    val textConstraints = constraints.copy(
        maxWidth = (containerWidthPx - edgePaddingPx * 2).coerceAtLeast(0),
    )

    val instructionPlaceable = measurables[0].measure(textConstraints)
    val errorPlaceable = measurables.getOrNull(1)?.measure(textConstraints)

    val coilBoxLeft = ((containerWidthPx - coilSizePx) / 2f * (1f + horizontalBias)).roundToInt()
    val coilBoxTop = ((containerHeightPx - coilSizePx) / 2f * (1f + verticalBias)).roundToInt()
    val coilCenterX = coilBoxLeft + coilSizePx / 2

    val instructionY = if (shouldRenderTextAboveCoil) {
        val textBlockHeight = instructionPlaceable.height +
            (errorPlaceable?.let { it.height + ErrorTextTopSpacing.roundToPx() } ?: 0)
        coilBoxTop - aboveCoilPadding(deviceRotation).roundToPx() - textBlockHeight
    } else {
        coilBoxTop + coilSizePx + bottomCoilPadding(deviceRotation).roundToPx()
    }

    return layout(containerWidthPx, containerHeightPx) {
        if (instructionPlaceable.width > 0 && instructionPlaceable.height > 0) {
            instructionPlaceable.placeRelative(
                x = clampedTextX(
                    placeable = instructionPlaceable,
                    coilCenterX = coilCenterX,
                    containerWidthPx = containerWidthPx,
                    edgePaddingPx = edgePaddingPx,
                ),
                y = instructionY,
            )
        }

        errorPlaceable?.let { placeable ->
            if (placeable.width > 0 && placeable.height > 0) {
                val errorY = instructionY +
                    instructionPlaceable.height +
                    ErrorTextTopSpacing.roundToPx()

                placeable.placeRelative(
                    x = clampedTextX(
                        placeable = placeable,
                        coilCenterX = coilCenterX,
                        containerWidthPx = containerWidthPx,
                        edgePaddingPx = edgePaddingPx,
                    ),
                    y = errorY,
                )
            }
        }
    }
}

private fun aboveCoilPadding(deviceRotation: DeviceRotation): Dp {
    return when (deviceRotation) {
        DeviceRotation.Portrait,
        DeviceRotation.UpsideDown -> PortraitTopTextOffset
        DeviceRotation.LandscapeLeft,
        DeviceRotation.LandscapeRight -> LandscapeTopTextOffset
    }
}

private fun bottomCoilPadding(deviceRotation: DeviceRotation): Dp {
    return when (deviceRotation) {
        DeviceRotation.Portrait,
        DeviceRotation.UpsideDown -> PortraitBottomTextOffset
        DeviceRotation.LandscapeLeft,
        DeviceRotation.LandscapeRight -> LandscapeBottomTextOffset
    }
}

private fun clampedTextX(
    placeable: Placeable,
    coilCenterX: Int,
    containerWidthPx: Int,
    edgePaddingPx: Int,
): Int {
    val desiredTextStartX = coilCenterX - placeable.width / 2
    return desiredTextStartX.coerceIn(
        edgePaddingPx,
        (containerWidthPx - edgePaddingPx - placeable.width).coerceAtLeast(edgePaddingPx),
    )
}

@Composable
private fun NfcCoilInstructionText(
    canShow: Boolean,
) {
    AnimatedVisibility(
        visible = canShow,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Text(
            text = stringResource(R.string.stripe_nfc_scan_hold_card_behind_phone),
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
    }
}
