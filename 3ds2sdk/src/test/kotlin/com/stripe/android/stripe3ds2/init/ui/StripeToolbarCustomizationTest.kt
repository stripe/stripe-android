package com.stripe.android.stripe3ds2.init.ui

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class StripeToolbarCustomizationTest {

    @Test
    fun testSettersGetters() {
        val toolbarCustomization = StripeToolbarCustomization()
        toolbarCustomization.setBackgroundColor("#abcdef")
        assertEquals("#abcdef", toolbarCustomization.backgroundColor)

        toolbarCustomization.setStatusBarColor("#beeeef")
        assertEquals("#beeeef", toolbarCustomization.statusBarColor)

        toolbarCustomization.setButtonText("Click me")
        assertEquals("Click me", toolbarCustomization.buttonText)

        toolbarCustomization.setHeaderText("Header")
        assertEquals("Header", toolbarCustomization.headerText)

        toolbarCustomization.setTextColor("#fafafa")
        assertEquals("#fafafa", toolbarCustomization.textColor)

        toolbarCustomization.setTextFontName("Arial")
        assertEquals("Arial", toolbarCustomization.textFontName)

        toolbarCustomization.textFontSize = 12
        assertEquals(12, toolbarCustomization.textFontSize.toLong())
    }

    @Test
    fun setBackgroundColor_invalidColor_shouldThrowException() {
        val toolbarCustomization = StripeToolbarCustomization()
        assertFailsWith<InvalidInputException> {
            toolbarCustomization.setBackgroundColor("#ghijkl")
        }
    }

    @Test
    fun setStatusBarColor_invalidColor_shouldThrowException() {
        val toolbarCustomization = StripeToolbarCustomization()
        assertFailsWith<InvalidInputException> {
            toolbarCustomization.setBackgroundColor("#EATEAT")
        }
    }

    @Test
    fun setTextColor_invalidColor_shouldThrowException() {
        val toolbarCustomization = StripeToolbarCustomization()
        assertFailsWith<InvalidInputException> {
            toolbarCustomization.setTextColor("#123XYZ")
        }
    }

    @Test
    fun setButtonText_emptyString_shouldThrowException() {
        val toolbarCustomization = StripeToolbarCustomization()
        assertFailsWith<InvalidInputException> {
            toolbarCustomization.setButtonText("")
        }
    }

    @Test
    fun setHeaderText_emptyString_shouldThrowException() {
        val toolbarCustomization = StripeToolbarCustomization()
        assertFailsWith<InvalidInputException> {
            toolbarCustomization.setHeaderText("")
        }
    }
}
