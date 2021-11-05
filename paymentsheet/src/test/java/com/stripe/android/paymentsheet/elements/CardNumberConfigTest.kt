package com.stripe.android.paymentsheet.elements

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import org.junit.Test

class CardNumberConfigTest {
    private val cardNumberConfig = CardNumberConfig()

    @Test
    fun `visualTransformation formats entered value`() {
        Truth.assertThat(cardNumberConfig.visualTransformation.filter(AnnotatedString("1234567890123456")).text)
            .isEqualTo(AnnotatedString("1234 5678 9012 3456"))
    }

    @Test
    fun `only numbers are allowed in the field`() {
        Truth.assertThat(cardNumberConfig.filter("123^@Number[\uD83E\uDD57."))
            .isEqualTo("123")
    }

    @Test
    fun `blank Number returns blank state`() {
        Truth.assertThat(cardNumberConfig.determineState(CardBrand.Visa, ""))
            .isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `card brand is invalid`() {
        val state = cardNumberConfig.determineState(CardBrand.Unknown, "0")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.card_number_invalid_brand)
    }

    @Test
    fun `incomplete number is in incomplete state`() {
        val state = cardNumberConfig.determineState(CardBrand.Visa, "12")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.card_number_incomplete)
    }

    @Test
    fun `card number is too long`() {
        val state = cardNumberConfig.determineState(CardBrand.Visa, "1234567890123456789")
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.card_number_longer_than_expected)
        Truth.assertThat(state.isFull()).isTrue()
        Truth.assertThat(state.isValid()).isTrue()
        Truth.assertThat(state.isBlank()).isFalse()
    }

    @Test
    fun `card number has invalid luhn`() {
        val state = cardNumberConfig.determineState(CardBrand.Visa, "4242424242424243")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.card_number_invalid_luhn)
    }

    @Test
    fun `card number is valid`() {
        val state = cardNumberConfig.determineState(CardBrand.Visa, "4242424242424242")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }
}
