package com.stripe.android.stripe3ds2.init.ui

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class StripeTextBoxCustomizationTest {

    @Test
    fun testSettersGetters() {
        val textBoxCustomization = StripeTextBoxCustomization()

        textBoxCustomization.borderWidth = 20
        assertEquals(20, textBoxCustomization.borderWidth)

        textBoxCustomization.setBorderColor("#ababab")
        assertEquals("#ababab", textBoxCustomization.borderColor)

        textBoxCustomization.cornerRadius = 16
        assertEquals(16, textBoxCustomization.cornerRadius)

        textBoxCustomization.setHintTextColor("#ff00ff")
        assertEquals("#ff00ff", textBoxCustomization.hintTextColor)
    }

    @Test
    fun setBorderColor_invalidColor_shouldThrowException() {
        val textBoxCustomization = StripeTextBoxCustomization()
        assertFailsWith<InvalidInputException> {
            textBoxCustomization.setBorderColor("123456")
        }
    }

    @Test
    fun setHintTextColor_invalidColor_shouldThrowException() {
        val textBoxCustomization = StripeTextBoxCustomization()
        assertFailsWith<InvalidInputException> {
            textBoxCustomization.setHintTextColor("123456")
        }
    }

    @Test
    fun setBorderWidth_lessThanZero_shouldThrowException() {
        val textBoxCustomization = StripeTextBoxCustomization()
        assertFailsWith<InvalidInputException> {
            textBoxCustomization.borderWidth = -1
        }
    }

    @Test
    fun setCornerRadius_lessThanZero_shouldThrowException() {
        val textBoxCustomization = StripeTextBoxCustomization()
        assertFailsWith<InvalidInputException> {
            textBoxCustomization.cornerRadius = -2
        }
    }
}
