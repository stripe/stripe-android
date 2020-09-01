package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CvcEditTextTest {

    private val cvcEditText = CvcEditText(ApplicationProvider.getApplicationContext())

    @Test
    fun cvcValue_withoutText_returnsNull() {
        assertThat(cvcEditText.cvc)
            .isNull()
    }

    @Test
    fun cvcValue_withValidVisaValue_returnsCvcValue() {
        cvcEditText.setText("123")
        cvcEditText.updateBrand(CardBrand.Visa)
        assertThat(cvcEditText.cvc?.value)
            .isEqualTo("123")
    }

    @Test
    fun cvcValue_withValidInvalidVisaValue_returnsCvcValue() {
        cvcEditText.setText("1234")
        cvcEditText.updateBrand(CardBrand.Visa)
        assertThat(cvcEditText.cvc)
            .isNull()
    }

    @Test
    fun cvcValue_withInvalidAmexValue_returnsCvcValue() {
        cvcEditText.setText("12")
        cvcEditText.updateBrand(CardBrand.AmericanExpress)
        assertThat(cvcEditText.cvc)
            .isNull()
    }

    @Test
    fun cvcValue_withValid3DigitAmexValue_returnsCvcValue() {
        cvcEditText.setText("123")
        cvcEditText.updateBrand(CardBrand.AmericanExpress)
        assertThat(cvcEditText.cvc?.value)
            .isEqualTo("123")
    }

    @Test
    fun cvcValue_withValid4DigitAmexValue_returnsCvcValue() {
        cvcEditText.setText("1234")
        cvcEditText.updateBrand(CardBrand.AmericanExpress)
        assertThat(cvcEditText.cvc?.value)
            .isEqualTo("1234")
    }

    @Test
    fun completionCallback_whenVisa_isInvoked_whenMax() {
        var hasCompleted = false

        cvcEditText.updateBrand(CardBrand.Visa)
        cvcEditText.completionCallback = { hasCompleted = true }

        cvcEditText.setText("1")
        assertFalse(hasCompleted)

        cvcEditText.setText("12")
        assertFalse(hasCompleted)

        cvcEditText.setText("123")
        assertTrue(hasCompleted)
    }

    @Test
    fun completionCallback_whenAmex_isInvoked_whenMax() {
        var hasCompleted = false

        cvcEditText.updateBrand(CardBrand.AmericanExpress)
        cvcEditText.completionCallback = { hasCompleted = true }

        cvcEditText.setText("1")
        assertFalse(hasCompleted)

        cvcEditText.setText("12")
        assertFalse(hasCompleted)

        cvcEditText.setText("123")
        assertFalse(hasCompleted)

        cvcEditText.setText("1234")
        assertTrue(hasCompleted)
    }
}
