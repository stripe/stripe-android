package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
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
        Truth.assertThat(cvcConfig.determineState(CardBrand.Visa, ""))
            .isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `card brand is invalid`() {
        val state = cvcConfig.determineState(CardBrand.Unknown, "0")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_card_number)
    }

    @Test
    fun `incomplete number is in incomplete state`() {
        val state = cvcConfig.determineState(CardBrand.Visa, "12")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_cvc)
    }

    @Test
    fun `cvc is too long`() {
        val state = cvcConfig.determineState(CardBrand.Visa, "1234567890123456789")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_cvc)
    }

    @Test
    fun `cvc is valid`() {
        var state = cvcConfig.determineState(CardBrand.Visa, "123")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)

        state = cvcConfig.determineState(CardBrand.AmericanExpress, "1234")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }
}
