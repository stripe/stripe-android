package com.stripe.android.common.nfcscan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.common.ui.InlineContentTemplateBuilder
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.strings.resolve
import kotlin.math.roundToInt
import com.stripe.payments.model.R as PaymentsModelR

private val TextAboveCoilBottomMargin = 22.5.dp
private val BottomTextOffset = 30.dp
private val InstructionTextEdgePadding = 20.dp
private val ErrorTextTopSpacing = 8.dp

@Composable
internal fun NfcCoilTextLayout(
    containerWidth: Dp,
    containerHeight: Dp,
    tapZone: TapZone,
    coilSize: Dp,
    shouldRenderTextAboveCoil: Boolean,
    canShow: Boolean,
    error: ResolvableString?,
) {
    Layout(
        content = {
            NfcCoilInstructionText(canShow = canShow)
            if (error != null) {
                NfcCoilErrorText(
                    message = error,
                    canShow = canShow,
                )
            }
        },
    ) { measurables, constraints ->
        placeCoilTextElements(
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            tapZone = tapZone,
            coilSize = coilSize,
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
        coilBoxTop - TextAboveCoilBottomMargin.roundToPx() - textBlockHeight
    } else {
        coilBoxTop + coilSizePx + BottomTextOffset.roundToPx()
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

@Composable
private fun NfcCoilErrorText(
    message: ResolvableString,
    canShow: Boolean,
) {
    AnimatedVisibility(
        visible = canShow,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val textStyle = MaterialTheme.typography.body1
        val fontSize = textStyle.fontSize

        Text(
            text = buildAnnotatedString {
                appendInlineContent(ERROR_ICON_ID)
                appendInlineContent(ERROR_SPACER_ID)
                append(message.resolve())
            },
            inlineContent = InlineContentTemplateBuilder()
                .add(
                    id = ERROR_ICON_ID,
                    width = fontSize,
                    height = fontSize,
                    align = PlaceholderVerticalAlign.TextCenter
                ) {
                    Icon(
                        painter = painterResource(PaymentsModelR.drawable.stripe_ic_error),
                        contentDescription = null,
                        tint = MaterialTheme.colors.error,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                .addSpacer(ERROR_SPACER_ID, ERROR_SPACER_WIDTH)
                .build(),
            color = MaterialTheme.colors.error,
            style = textStyle,
            textAlign = TextAlign.Center,
        )
    }
}

private const val ERROR_SPACER_ID = "ERROR_SPACER"
private val ERROR_SPACER_WIDTH = 5.sp

private const val ERROR_ICON_ID = "ERROR_ICON_ID"
