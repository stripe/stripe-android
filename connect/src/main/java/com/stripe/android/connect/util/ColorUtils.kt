package com.stripe.android.connect.util

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import kotlin.math.max
import kotlin.math.min

/**
 * Returns a color for minimum contrast with a given background color
 *
 * Reference: [WCAG 2.1 Contrast Minimum](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html#dfn-contrast-ratio)
 *
 * @param color The background color with which to get minimum contrast
 * @param minimumRatio The minimum contrast ratio (defaults to WCAG minimum ratio of 4.5)
 * @return The adjusted color that meets the minimum contrast ratio
 */
@Suppress("MagicNumber", "ComplexCondition")
@ColorInt
fun getContrastingColor(@ColorInt color: Int, minimumRatio: Float = 4.5f): Int {
    var adjustedColor = color

    val shouldLighten = ColorUtils.calculateLuminance(color) < 0.5
    val hsv = FloatArray(3)
    Color.colorToHSV(adjustedColor, hsv)

    while (
        ColorUtils.calculateContrast(adjustedColor, color) < minimumRatio &&
        ((shouldLighten && hsv[2] < 1f) || (!shouldLighten && hsv[2] > 0f))
    ) {
        if (shouldLighten) {
            hsv[2] = min(1f, hsv[2] + HSV_VALUE_STEP_SIZE)
        } else {
            hsv[2] = max(0f, hsv[2] - HSV_VALUE_STEP_SIZE)
        }
        adjustedColor = Color.HSVToColor(hsv)
    }

    return adjustedColor
}

private const val HSV_VALUE_STEP_SIZE = 0.1f
