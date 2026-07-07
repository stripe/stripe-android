package com.stripe.android.common.nfcscan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.paymentsheet.R
import kotlin.math.roundToInt

private val TopTextOffset = (-55).dp
private val BottomTextOffset = 30.dp
private val InstructionTextEdgePadding = 20.dp

@Composable
internal fun NfcCoilInstructionTextLayout(
    containerWidth: Dp,
    containerHeight: Dp,
    tapZone: TapZone,
    coilSize: Dp,
    shouldRenderTextAboveCoil: Boolean,
    canShow: Boolean,
) {
    val horizontalBias = tapZone.xBias * 2 - 1
    val verticalBias = tapZone.yBias * 2 - 1

    Layout(
        content = {
            NfcCoilInstructionText(canShow = canShow)
        },
    ) { measurables, constraints ->
        val containerWidthPx = containerWidth.roundToPx()
        val containerHeightPx = containerHeight.roundToPx()

        if (measurables.isEmpty()) {
            return@Layout layout(containerWidthPx, containerHeightPx) {}
        }

        val textPlaceable = measurables[0].measure(constraints)
        val coilSizePx = coilSize.roundToPx()
        val edgePaddingPx = InstructionTextEdgePadding.roundToPx()

        val coilBoxLeft = ((containerWidthPx - coilSizePx) / 2f * (1f + horizontalBias)).roundToInt()
        val coilBoxTop = ((containerHeightPx - coilSizePx) / 2f * (1f + verticalBias)).roundToInt()
        val coilCenterX = coilBoxLeft + coilSizePx / 2

        val desiredTextStartX = coilCenterX - textPlaceable.width / 2
        val textX = desiredTextStartX.coerceIn(
            edgePaddingPx,
            (containerWidthPx - edgePaddingPx - textPlaceable.width).coerceAtLeast(edgePaddingPx),
        )

        val textY = if (shouldRenderTextAboveCoil) {
            coilBoxTop + TopTextOffset.roundToPx()
        } else {
            coilBoxTop + coilSizePx + BottomTextOffset.roundToPx()
        }

        layout(containerWidthPx, containerHeightPx) {
            textPlaceable.placeRelative(textX, textY)
        }
    }
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
