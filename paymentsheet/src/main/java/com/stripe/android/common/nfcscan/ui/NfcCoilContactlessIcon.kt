package com.stripe.android.common.nfcscan.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.PrimaryButtonTheme

@Composable
internal fun NfcCoilContactlessIcon(
    modifier: Modifier = Modifier,
) {
    val tintColor = PrimaryButtonTheme.colors.onBackground

    if (LocalInspectionMode.current) {
        IconCanvas(
            modifier = modifier.testTag(NFC_COIL_CONTACTLESS_ICON_TEST_TAG),
            progress = MAX_PROGRESS_VALUE,
            tintColor = tintColor,
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "nfc_icon_shimmer")
    val progress by transition.animateFloat(
        initialValue = MIN_PROGRESS_VALUE,
        targetValue = MAX_PROGRESS_VALUE,
        animationSpec = infiniteRepeatable(
            animation = tween(ANIMATION_TIME_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "nfc_icon_shimmer_progress",
    )

    IconCanvas(
        modifier = modifier.testTag(NFC_COIL_CONTACTLESS_ICON_TEST_TAG),
        progress = progress,
        tintColor = tintColor,
    )
}

@Composable
private fun IconCanvas(
    modifier: Modifier,
    progress: Float,
    tintColor: Color,
) {
    val colorFilter = remember(tintColor) { ColorFilter.tint(tintColor) }
    val barPainters = BARS_WITH_LOBE_STOPS.map { painterResource(it.barRes) }

    Canvas(modifier = modifier) {
        BARS_WITH_LOBE_STOPS.forEachIndexed { index, data ->
            with(barPainters[index]) {
                draw(
                    size = size,
                    alpha = piecewise(progress, data.stops, data.values),
                    colorFilter = colorFilter,
                )
            }
        }
    }
}

private fun piecewise(t: Float, stops: FloatArray, values: FloatArray): Float {
    if (t <= stops.first()) return values.first()
    if (t >= stops.last()) return values.last()

    for (i in 0 until stops.size - 1) {
        if (t >= stops[i] && t <= stops[i + 1]) {
            val f = (t - stops[i]) / (stops[i + 1] - stops[i])
            return values[i] + (values[i + 1] - values[i]) * f
        }
    }

    return values.last()
}

private class BarAnimationData(
    @DrawableRes val barRes: Int,
    val stops: FloatArray,
    val values: FloatArray,
)

// Per-lobe shimmer keyframes for each NFC bar — heavily overlapping for a flowing wave feel.
// Higher base opacity (0.45) so lobes never fully dim; gentle peak (1.0).
// Each lobe is offset by ~0.2 of the cycle — close enough to blur together.
private val BARS_WITH_LOBE_STOPS = listOf(
    // Lobe 1 (innermost): peaks at ~0.15, wraps around end
    BarAnimationData(
        barRes = R.drawable.stripe_ic_material_nfc_coil_bar1,
        stops = floatArrayOf(0f, 0.15f, 0.40f, 0.70f, 0.90f, 1f),
        values = floatArrayOf(1f, 1f, 0.50f, 0.45f, 0.65f, 1f),
    ),
    // Lobe 2 (middle): peaks at ~0.35
    BarAnimationData(
        barRes = R.drawable.stripe_ic_material_nfc_coil_bar2,
        stops = floatArrayOf(0f, 0.10f, 0.35f, 0.60f, 0.85f, 1f),
        values = floatArrayOf(0.55f, 0.65f, 1f, 0.55f, 0.45f, 0.55f),
    ),
    // Lobe 3 (outermost): peaks at ~0.55
    BarAnimationData(
        barRes = R.drawable.stripe_ic_material_nfc_coil_bar3,
        stops = floatArrayOf(0f, 0.15f, 0.35f, 0.55f, 0.75f, 1f),
        values = floatArrayOf(0.45f, 0.45f, 0.65f, 1f, 0.60f, 0.45f),
    ),
)

internal const val NFC_COIL_CONTACTLESS_ICON_TEST_TAG = "nfc_coil_contactless_icon"

private const val ANIMATION_TIME_MS = 2000

private const val MIN_PROGRESS_VALUE = 0f
private const val MAX_PROGRESS_VALUE = 1f
