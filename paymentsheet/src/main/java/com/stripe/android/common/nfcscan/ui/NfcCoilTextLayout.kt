package com.stripe.android.common.nfcscan.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.strings.resolve
import kotlin.math.roundToInt

private val PortraitTopTextOffset = 40.5.dp
private val PortraitBottomTextOffset = 48.dp
private val LandscapeTopTextOffset = 22.5.dp
private val LandscapeBottomTextOffset = 30.dp
private val InstructionTextEdgePadding = 20.dp
private val ErrorTextTopSpacing = 8.dp

@Composable
internal fun NfcCoilTextLayout(
    text: ResolvableString,
    subtitle: ResolvableString?,
    containerWidth: Dp,
    containerHeight: Dp,
    tapZone: TapZone,
    coilSize: Dp,
    deviceRotation: DeviceRotation,
    shouldRenderTextAboveCoil: Boolean,
) {
    Layout(
        content = {
            NfcText(text, subtitle)
        },
    ) { measurables, constraints ->
        placeCoilTextElements(
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            tapZone = tapZone,
            coilSize = coilSize,
            shouldRenderTextAboveCoil = shouldRenderTextAboveCoil,
            measurables = measurables,
            deviceRotation = deviceRotation,
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
    deviceRotation: DeviceRotation,
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
internal fun NfcText(
    text: ResolvableString,
    subtitle: ResolvableString?,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Rolodex(
            text = text.resolve(),
            modifier = modifier,
        ) { text, modifier ->
            Text(
                text = text,
                color = MaterialTheme.colors.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
                textAlign = TextAlign.Center,
                modifier = modifier,
            )
        }

        Spacer(Modifier.size(8.dp))

        Rolodex(
            text = subtitle?.resolve() ?: "",
            modifier = modifier,
        ) { text, modifier ->
            Text(
                text = text,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun Rolodex(
    text: String,
    modifier: Modifier = Modifier,
    textContent: @Composable (text: String, modifier: Modifier) -> Unit,
) {
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            val enterTransition = slideInVertically(
                animationSpec = RolodexEnterOffsetAnimationSpec,
                initialOffsetY = { fullHeight ->
                    -(fullHeight * ROLODEX_OFFSET_FRACTION).toInt()
                },
            ) + fadeIn(animationSpec = RolodexEnterAlphaAnimationSpec)

            val exitTransition = slideOutVertically(
                animationSpec = RolodexExitOffsetAnimationSpec,
                targetOffsetY = { fullHeight ->
                    (fullHeight * ROLODEX_OFFSET_FRACTION).toInt()
                },
            ) + fadeOut(animationSpec = RolodexExitAlphaAnimationSpec)

            enterTransition togetherWith exitTransition using SizeTransform(
                sizeAnimationSpec = { _, _ ->
                    snap(delayMillis = DURATION_MILLIS)
                },
                clip = false,
            )
        },
        label = "RolodexAnimation",
    ) { content ->
        textContent(
            content,
            Modifier.graphicsLayer {
                rotationX = ROTATION_X
                cameraDistance = CAMERA_DISTANCE * density
            },
        )
    }
}

private val RolodexEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private val RolodexExitOffsetAnimationSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = DURATION_MILLIS, easing = RolodexEasing)
private val RolodexEnterOffsetAnimationSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = DURATION_MILLIS, delayMillis = DURATION_MILLIS, easing = RolodexEasing)
private val RolodexExitAlphaAnimationSpec: FiniteAnimationSpec<Float> =
    tween(durationMillis = DURATION_MILLIS, easing = RolodexEasing)
private val RolodexEnterAlphaAnimationSpec: FiniteAnimationSpec<Float> =
    tween(durationMillis = DURATION_MILLIS, delayMillis = DURATION_MILLIS, easing = RolodexEasing)

private const val ROTATION_X = 0f
private const val ROLODEX_OFFSET_FRACTION = 0.235f
private const val DURATION_MILLIS = 400
private const val CAMERA_DISTANCE = 12f
