package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.CardBrand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CvcEditTextTest {

    private val cvcEditText: CvcEditText by lazy {
        CvcEditText(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun cvcValue_withoutText_returnsNull() {
        assertNull(cvcEditText.cvcValue)
    }

    @Test
    fun cvcValue_withValidVisaValue_returnsCvcValue() {
        cvcEditText.setText("123")
        cvcEditText.updateBrand(CardBrand.Visa)
        assertEquals("123", cvcEditText.cvcValue)
    }

    @Test
    fun cvcValue_withValidInvalidVisaValue_returnsCvcValue() {
        cvcEditText.setText("1234")
        cvcEditText.updateBrand(CardBrand.Visa)
        assertNull(cvcEditText.cvcValue)
    }

    @Test
    fun cvcValue_withInvalidAmexValue_returnsCvcValue() {
        cvcEditText.setText("12")
        cvcEditText.updateBrand(CardBrand.AmericanExpress)
        assertNull(cvcEditText.cvcValue)
    }

    @Test
    fun cvcValue_withValid3DigitAmexValue_returnsCvcValue() {
        cvcEditText.setText("123")
        cvcEditText.updateBrand(CardBrand.AmericanExpress)
        assertEquals("123", cvcEditText.cvcValue)
    }

    @Test
    fun cvcValue_withValid4DigitAmexValue_returnsCvcValue() {
        cvcEditText.setText("1234")
        cvcEditText.updateBrand(CardBrand.AmericanExpress)
        assertEquals("1234", cvcEditText.cvcValue)
    }

    @Test
    fun completionCallback_isInvoked_whenValid() {
        var hasCompleted = false
        cvcEditText.completionCallback = { hasCompleted = true }

        cvcEditText.setText("1")
        assertFalse(hasCompleted)

        cvcEditText.setText("12")
        assertFalse(hasCompleted)

        cvcEditText.setText("123")
        assertTrue(hasCompleted)
    }
}
