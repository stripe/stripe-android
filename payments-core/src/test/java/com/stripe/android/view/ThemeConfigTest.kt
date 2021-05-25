package com.stripe.android.view

import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ThemeConfigTest {

    private val themeConfig = ThemeConfig(ApplicationProvider.getApplicationContext())

    @Test
    fun textColorValues_shouldBeExpectedValues() {
        val alpha = 204 // 80% of 255
        val colorValues = themeConfig.textColorValues
        // The colors are arranged [selected, selectedLowAlpha, unselected, unselectedLowAlpha
        assertEquals(4, colorValues.size)
        assertEquals(
            colorValues[1],
            ColorUtils.setAlphaComponent(colorValues[0], alpha)
        )
        assertEquals(
            colorValues[3],
            ColorUtils.setAlphaComponent(colorValues[2], alpha)
        )
    }
}
