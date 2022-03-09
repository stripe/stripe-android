package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.CardNumberFixtures
import com.stripe.android.ui.core.R
import org.junit.Test

class CardNumberConfigTest {
    private val cardNumberConfig = CardNumberConfig()

    @Test
    fun `visualTransformation formats entered value`() {
        Truth.assertThat(cardNumberConfig.visualTransformation.filter(AnnotatedString(CardNumberFixtures.VISA_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.VISA_WITH_SPACES))

        Truth.assertThat(cardNumberConfig.visualTransformation.filter(AnnotatedString(CardNumberFixtures.AMEX_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.AMEX_WITH_SPACES))

        Truth.assertThat(cardNumberConfig.visualTransformation.filter(AnnotatedString(CardNumberFixtures.DISCOVER_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.DISCOVER_WITH_SPACES))

        Truth.assertThat(cardNumberConfig.visualTransformation.filter(AnnotatedString(CardNumberFixtures.DINERS_CLUB_14_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.DINERS_CLUB_14_WITH_SPACES))

        Truth.assertThat(cardNumberConfig.visualTransformation.filter(AnnotatedString(CardNumberFixtures.DINERS_CLUB_16_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.DINERS_CLUB_16_WITH_SPACES))

        Truth.assertThat(cardNumberConfig.visualTransformation.filter(AnnotatedString(CardNumberFixtures.JCB_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.JCB_WITH_SPACES))

        Truth.assertThat(cardNumberConfig.visualTransformation.filter(AnnotatedString(CardNumberFixtures.UNIONPAY_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.UNIONPAY_WITH_SPACES))
    }

    @Test
    fun `only numbers are allowed in the field`() {
        Truth.assertThat(cardNumberConfig.filter("123^@Number[\uD83E\uDD57."))
            .isEqualTo("123")
    }

    @Test
    fun `blank Number returns blank state`() {
        Truth.assertThat(cardNumberConfig.determineState(CardBrand.Visa, "", CardBrand.Visa.getMaxLengthForCardNumber("")))
            .isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `card brand is invalid`() {
        val state = cardNumberConfig.determineState(CardBrand.Unknown, "0", CardBrand.Unknown.getMaxLengthForCardNumber("0"))
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_card_number)
    }

    @Test
    fun `incomplete number is in incomplete state`() {
        val state = cardNumberConfig.determineState(CardBrand.Visa, "12", CardBrand.Visa.getMaxLengthForCardNumber("12"))
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_card_number)
    }

    @Test
    fun `card number is too long`() {
        val state = cardNumberConfig.determineState(CardBrand.Visa, "1234567890123456789", CardBrand.Visa.getMaxLengthForCardNumber("1234567890123456789"))
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_card_number)
    }

    @Test
    fun `card number has invalid luhn`() {
        val state = cardNumberConfig.determineState(CardBrand.Visa, "4242424242424243", CardBrand.Visa.getMaxLengthForCardNumber("4242424242424243"))
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_card_number)
    }

    @Test
    fun `card number is valid`() {
        val state = cardNumberConfig.determineState(CardBrand.Visa, "4242424242424242", CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242"))
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }
}
