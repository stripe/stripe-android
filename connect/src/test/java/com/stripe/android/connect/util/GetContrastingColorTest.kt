package com.stripe.android.connect.util

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetContrastingColorTest {
    @Test
    fun `should not loop infinitely`() {
        val midGray = Color.argb(1f, .5f, .5f, .5f)

        // The maximum contrast ratio to mid-gray is 5.28, ensure that this
        // returns a color with the maximum contrast ratio that can be achieved
        val color = getContrastingColor(midGray, 5.5f)
        assertThat(ColorUtils.calculateContrast(color, midGray)).isLessThan(5.5)
        assertThat(color).isEqualTo(Color.WHITE)
    }

    @Test
    fun `should return a contrasting color`() {
        listOf(
            Color.WHITE,
            Color.LTGRAY,
            Color.CYAN,
            Color.DKGRAY,
            Color.BLACK,
        ).forEach { bgColor ->
            val color = getContrastingColor(bgColor, 4.5f)
            val contrast = ColorUtils.calculateContrast(color, bgColor)
            assertThat(contrast).isGreaterThan(4.5)
            assertThat(contrast).isLessThan(6.0)
        }
    }
}
