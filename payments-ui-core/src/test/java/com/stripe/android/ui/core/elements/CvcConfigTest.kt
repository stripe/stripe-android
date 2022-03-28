package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.R
import org.junit.Test

class CvcConfigTest {
    private val cvcConfig = CvcConfig()

    @Test
    fun `only numbers are allowed in the field`() {
        Truth.assertThat(cvcConfig.filter("123^@Number[\uD83E\uDD57."))
            .isEqualTo("123")
    }

    @Test
    fun `blank Number returns blank state`() {
        Truth.assertThat(cvcConfig.determineState(CardBrand.Visa, "", CardBrand.Visa.maxCvcLength))
            .isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `card brand is invalid`() {
        val state = cvcConfig.determineState(CardBrand.Unknown, "0", CardBrand.Unknown.maxCvcLength)
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Valid.Limitless::class.java)
    }

    @Test
    fun `incomplete number is in incomplete state`() {
        val state = cvcConfig.determineState(CardBrand.Visa, "12", CardBrand.Visa.maxCvcLength)
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_cvc)
    }

    @Test
    fun `cvc is too long`() {
        val state = cvcConfig.determineState(CardBrand.Visa, "1234567890123456789", CardBrand.Visa.maxCvcLength)
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_cvc)
    }

    @Test
    fun `cvc is valid`() {
        var state = cvcConfig.determineState(CardBrand.Visa, "123", CardBrand.Visa.maxCvcLength)
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)

        state = cvcConfig.determineState(CardBrand.AmericanExpress, "1234", CardBrand.AmericanExpress.maxCvcLength)
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }
}
