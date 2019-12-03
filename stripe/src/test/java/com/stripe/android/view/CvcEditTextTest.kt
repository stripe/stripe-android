package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.Card
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
        cvcEditText.updateBrand(Card.CardBrand.VISA)
        assertEquals("123", cvcEditText.cvcValue)
    }

    @Test
    fun cvcValue_withValidInvalidVisaValue_returnsCvcValue() {
        cvcEditText.setText("1234")
        cvcEditText.updateBrand(Card.CardBrand.VISA)
        assertNull(cvcEditText.cvcValue)
    }

    @Test
    fun cvcValue_withInvalidAmexValue_returnsCvcValue() {
        cvcEditText.setText("12")
        cvcEditText.updateBrand(Card.CardBrand.AMERICAN_EXPRESS)
        assertNull(cvcEditText.cvcValue)
    }

    @Test
    fun cvcValue_withValid3DigitAmexValue_returnsCvcValue() {
        cvcEditText.setText("123")
        cvcEditText.updateBrand(Card.CardBrand.AMERICAN_EXPRESS)
        assertEquals("123", cvcEditText.cvcValue)
    }

    @Test
    fun cvcValue_withValid4DigitAmexValue_returnsCvcValue() {
        cvcEditText.setText("1234")
        cvcEditText.updateBrand(Card.CardBrand.AMERICAN_EXPRESS)
        assertEquals("1234", cvcEditText.cvcValue)
    }
}
