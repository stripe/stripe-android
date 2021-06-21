package com.stripe.android.view

import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CvcEditTextTest {
    private val cvcEditText = CvcEditText(
        ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.StripeDefaultTheme
        )
    )

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
    fun `CvcEditText should remove non-digits from input`() {
        cvcEditText.append("-1.2")
        cvcEditText.append("a")
        cvcEditText.append("3")
        assertThat(cvcEditText.fieldText)
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

    @Test
    fun cvcCustomPlaceholderSet_usesCustomPlaceholder() {
        val textInputLayout = TextInputLayout(
            ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.StripeDefaultTheme
            )
        )
        cvcEditText.updateBrand(
            CardBrand.AmericanExpress,
            customPlaceholderText = "custom placeholder",
            textInputLayout = textInputLayout
        )
        assertThat(textInputLayout.placeholderText).isEqualTo("custom placeholder")

        cvcEditText.updateBrand(
            CardBrand.AmericanExpress,
            textInputLayout = textInputLayout
        )
        assertThat(textInputLayout.placeholderText).isEqualTo("1234")
    }

    @Test
    fun `when lose focus and cvc length is wrong, show error`() {
        cvcEditText.setText("12")
        cvcEditText.updateBrand(CardBrand.AmericanExpress)
        cvcEditText.onFocusChangeListener?.onFocusChange(cvcEditText, false)
        assertThat(cvcEditText.shouldShowError)
            .isTrue()
    }
}
