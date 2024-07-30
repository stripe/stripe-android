package com.stripe.android.stripe3ds2.init.ui

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class StripeLabelCustomizationTest {

    @Test
    fun testSettersGetters() {
        val labelCustomization = StripeLabelCustomization()

        labelCustomization.setHeadingTextColor("#eaeaea")
        assertEquals("#eaeaea", labelCustomization.headingTextColor)

        labelCustomization.setHeadingTextFontName("Arial")
        assertEquals("Arial", labelCustomization.headingTextFontName)

        labelCustomization.headingTextFontSize = 16
        assertEquals(16, labelCustomization.headingTextFontSize.toLong())
    }

    @Test
    fun setHeadingTextColor_invalidColor_shouldThrowException() {
        val labelCustomization = StripeLabelCustomization()
        assertFailsWith<InvalidInputException> {
            labelCustomization.setHeadingTextColor("#FFF")
        }
    }

    @Test
    fun setHeadingTextFontName_emptyString_shouldThrowException() {
        val labelCustomization = StripeLabelCustomization()
        assertFailsWith<InvalidInputException> {
            labelCustomization.setHeadingTextFontName("")
        }
    }

    @Test
    fun setHeadingTextFontSize_lessThanZero_shouldThrowException() {
        val labelCustomization = StripeLabelCustomization()
        assertFailsWith<InvalidInputException> {
            labelCustomization.headingTextFontSize = -1
        }
    }
}
