package com.stripe.android.stripe3ds2.utils

import android.graphics.Color
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import androidx.annotation.ColorInt
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CustomizeUtilsTest {

    @Test
    fun darken_one_sameColor() {
        @ColorInt val color = Color.parseColor("#6C00F8")
        assertEquals(color.toLong(), CustomizeUtils.darken(color, 1f).toLong())
    }

    @Test
    fun darken_max_maxedOutColor() {
        @ColorInt val color = Color.parseColor("#6C00F8")
        assertEquals(Color.rgb(255, 0, 255).toLong(), CustomizeUtils.darken(color, 255f).toLong())
    }

    @Test
    fun darken_min_minColor() {
        @ColorInt val color = Color.parseColor("#6C00F8")
        assertEquals(Color.rgb(0, 0, 0).toLong(), CustomizeUtils.darken(color, 0f).toLong())
    }

    @Test
    fun darken_zeroPointEight_darkenedColor() {
        @ColorInt val color = Color.parseColor("#6C00F8")
        assertEquals(Color.rgb(86, 0, 198).toLong(), CustomizeUtils.darken(color, 0.8f).toLong())
    }

    @Test
    fun buildStyledText_textIsStyled() {
        val toolbarCustomization = StripeToolbarCustomization()
        toolbarCustomization.setTextColor("#FFFFFF")
        toolbarCustomization.textFontSize = 16
        toolbarCustomization.setTextFontName("serif")

        val styledText = CustomizeUtils.buildStyledText(
            ApplicationProvider.getApplicationContext(), "HEADER", toolbarCustomization
        )

        val colorSpans = styledText.getSpans(0, 5, ForegroundColorSpan::class.java)
        assertEquals(1, colorSpans.size.toLong())
        assertEquals(Color.WHITE.toLong(), colorSpans[0].foregroundColor.toLong())

        val sizeSpans = styledText.getSpans(0, 5, AbsoluteSizeSpan::class.java)
        assertEquals(1, sizeSpans.size.toLong())
        assertEquals(16, sizeSpans[0].size.toLong())

        val fontSpans = styledText.getSpans(0, 5, TypefaceSpan::class.java)
        assertEquals(1, fontSpans.size.toLong())
        assertEquals("serif", fontSpans[0].family)
    }

    @Test
    fun colorIntToHex_correctStringValuesReturned() {
        assertEquals(
            "#FF000000",
            CustomizeUtils.colorIntToHex(Color.parseColor("#FF000000"))
        )
        assertEquals(
            "#00FFFFFF",
            CustomizeUtils.colorIntToHex(Color.parseColor("#00FFFFFF"))
        )
        assertEquals(
            "#FFFFFFFF",
            CustomizeUtils.colorIntToHex(Color.parseColor("#FFFFFFFF"))
        )
        assertEquals(
            "#00000000",
            CustomizeUtils.colorIntToHex(Color.parseColor("#00000000"))
        )
        assertEquals(
            "#12345678",
            CustomizeUtils.colorIntToHex(Color.parseColor("#12345678"))
        )
    }
}
