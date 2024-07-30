package com.stripe.android.stripe3ds2.init.ui

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class StripeButtonCustomizationTest {

    @Test
    fun testSettersGetters() {
        val buttonCustomization = StripeButtonCustomization()

        buttonCustomization.setBackgroundColor("#eaeaea")
        assertEquals("#eaeaea", buttonCustomization.backgroundColor)

        buttonCustomization.cornerRadius = 16
        assertEquals(16, buttonCustomization.cornerRadius.toLong())
    }

    @Test
    fun setBackgroundColor_invalidColor_shouldThrowException() {
        val buttonCustomization = StripeButtonCustomization()
        assertFailsWith<InvalidInputException> {
            buttonCustomization.setBackgroundColor("#123")
        }
    }

    @Test
    fun setCornerRadius_lessThanZero_shouldThrowException() {
        val buttonCustomization = StripeButtonCustomization()
        assertFailsWith<InvalidInputException> {
            buttonCustomization.cornerRadius = -1
        }
    }

    @Test
    fun setTextColor_invalidColor_shouldThrowException() {
        val buttonCustomization = StripeButtonCustomization()
        assertFailsWith<InvalidInputException> {
            buttonCustomization.setTextColor("#ZXC123")
        }
    }

    @Test
    fun setTextFontName_emptyString_shouldThrowException() {
        val buttonCustomization = StripeButtonCustomization()
        assertFailsWith<InvalidInputException> {
            buttonCustomization.setTextFontName("")
        }
    }

    @Test
    fun setTextFontSize_zero_shouldThrowException() {
        val buttonCustomization = StripeButtonCustomization()
        assertFailsWith<InvalidInputException> {
            buttonCustomization.textFontSize = 0
        }
    }
}
