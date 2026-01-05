package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import com.stripe.android.testing.FakeCardFundingFilter
import com.stripe.android.ui.core.CardNumberFixtures
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.utils.FakeCardBrandFilter
import com.stripe.android.utils.isInstanceOf
import org.junit.Test
import com.stripe.android.R as StripeR

class CardNumberConfigTest {

    private val cardBrandChoiceOptions = listOf(true, false)

    @Test
    fun `visualTransformation are created correctly`() {
        for (isCardBrandChoiceEligible in cardBrandChoiceOptions) {
            val cardNumberConfig = cardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )

            assertThat(
                cardNumberConfig.determineVisualTransformation(
                    number = CardNumberFixtures.VISA_NO_SPACES,
                    panLength = 16
                )
            ).isEqualTo(CardNumberVisualTransformations.Default(separator = ' '))

            assertThat(
                cardNumberConfig.determineVisualTransformation(
                    number = CardNumberFixtures.DINERS_CLUB_14_NO_SPACES,
                    panLength = 14
                )
            ).isEqualTo(CardNumberVisualTransformations.FourteenAndFifteenPanLength(separator = ' '))

            assertThat(
                cardNumberConfig.determineVisualTransformation(
                    number = CardNumberFixtures.AMEX_NO_SPACES,
                    panLength = 15
                )
            ).isEqualTo(CardNumberVisualTransformations.FourteenAndFifteenPanLength(separator = ' '))

            assertThat(
                cardNumberConfig.determineVisualTransformation(
                    number = CardNumberFixtures.UNIONPAY_NO_SPACES,
                    panLength = 19
                )
            ).isEqualTo(CardNumberVisualTransformations.NineteenPanLength(separator = ' '))

            assertThat(
                cardNumberConfig.determineVisualTransformation(
                    number = CardNumberFixtures.AMEX_NO_SPACES,
                    panLength = 15
                )
            ).isEqualTo(CardNumberVisualTransformations.FourteenAndFifteenPanLength(separator = ' '))

            assertThat(
                cardNumberConfig.determineVisualTransformation(
                    number = CardNumberFixtures.VISA_NO_SPACES,
                    panLength = 20
                )
            ).isEqualTo(CardNumberVisualTransformations.Default(separator = ' '))

            assertThat(
                cardNumberConfig.determineVisualTransformation(
                    number = CardNumberFixtures.VISA_NO_SPACES,
                    panLength = 13
                )
            ).isEqualTo(CardNumberVisualTransformations.Default(separator = ' '))
        }
    }

    @Test
    fun `only numbers are allowed in the field`() {
        for (isCardBrandChoiceEligible in cardBrandChoiceOptions) {
            val cardNumberConfig = cardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )
            assertThat(cardNumberConfig.filter("123^@Number[\uD83E\uDD57."))
                .isEqualTo("123")
        }
    }

    @Test
    fun `blank Number returns blank state`() {
        for (isCardBrandChoiceEligible in cardBrandChoiceOptions) {
            val cardNumberConfig = cardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )
            assertThat(
                cardNumberConfig.determineState(
                    brand = CardBrand.Visa,
                    funding = null,
                    number = "",
                    numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("")
                )
            )
                .isEqualTo(TextFieldStateConstants.Error.Blank)
        }
    }

    @Test
    fun `card brand is invalid`() {
        for (isCardBrandChoiceEligible in cardBrandChoiceOptions) {
            val cardNumberConfig = cardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )
            val state = cardNumberConfig.determineState(
                brand = CardBrand.Unknown,
                funding = null,
                number = "0",
                numberAllowedDigits = CardBrand.Unknown.getMaxLengthForCardNumber("0")
            )
            assertThat(state)
                .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
            assertThat(
                state.getError()?.errorMessage
            ).isEqualTo(StripeR.string.stripe_invalid_card_number)
        }
    }

    @Test
    fun `incomplete number is in incomplete state`() {
        for (isCardBrandChoiceEligible in cardBrandChoiceOptions) {
            val cardNumberConfig = cardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )
            val state = cardNumberConfig.determineState(
                brand = CardBrand.Visa,
                funding = null,
                number = "12",
                numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("12")
            )
            assertThat(state)
                .isInstanceOf<TextFieldStateConstants.Error.Incomplete>()
            assertThat(
                state.getError()?.errorMessage
            ).isEqualTo(StripeR.string.stripe_invalid_card_number)
        }
    }

    @Test
    fun `card number is too long`() {
        for (isCardBrandChoiceEligible in cardBrandChoiceOptions) {
            val cardNumberConfig = cardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )
            val state = cardNumberConfig.determineState(
                brand = CardBrand.Visa,
                funding = null,
                number = "1234567890123456789",
                numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("1234567890123456789")
            )
            assertThat(state)
                .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
            assertThat(
                state.getError()?.errorMessage
            ).isEqualTo(StripeR.string.stripe_invalid_card_number)
        }
    }

    @Test
    fun `card number has invalid luhn`() {
        for (isCardBrandChoiceEligible in cardBrandChoiceOptions) {
            val cardNumberConfig = cardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )
            val state = cardNumberConfig.determineState(
                brand = CardBrand.Visa,
                funding = null,
                number = "4242424242424243",
                numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424243")
            )
            assertThat(state)
                .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
            assertThat(
                state.getError()?.errorMessage
            ).isEqualTo(StripeR.string.stripe_invalid_card_number)
        }
    }

    @Test
    fun `card number is valid`() {
        for (isCardBrandChoiceEligible in cardBrandChoiceOptions) {
            val cardNumberConfig = cardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )
            val state = cardNumberConfig.determineState(
                brand = CardBrand.Visa,
                funding = null,
                number = "4242424242424242",
                numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242")
            )
            assertThat(state)
                .isInstanceOf<TextFieldStateConstants.Valid.Full>()
        }
    }

    @Test
    fun `determineState returns valid for allowed brand without CBC`() {
        val cardBrandFilter = FakeCardBrandFilter(disallowedBrands = setOf(CardBrand.MasterCard))
        val cardNumberConfig = cardNumberConfig(
            isCardBrandChoiceEligible = false,
            cardBrandFilter = cardBrandFilter
        )

        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            funding = null,
            number = "4242424242424242",
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242")
        )

        assertThat(state).isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    @Test
    fun `determineState returns error for disallowed brand without CBC`() {
        val cardBrandFilter = FakeCardBrandFilter(disallowedBrands = setOf(CardBrand.MasterCard))
        val cardNumberConfig = cardNumberConfig(
            isCardBrandChoiceEligible = false,
            cardBrandFilter = cardBrandFilter
        )

        val state = cardNumberConfig.determineState(
            brand = CardBrand.MasterCard,
            funding = null,
            number = "5555555555554444",
            numberAllowedDigits = CardBrand.MasterCard.getMaxLengthForCardNumber("5555555555554444")
        )

        assertThat(state).isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        assertThat(state.getError()?.errorMessage)
            .isEqualTo(StripeR.string.stripe_disallowed_card_brand)
    }

    @Test
    fun `determineState allows disallowed brand with CBC when number length is less than or equal to 8`() {
        val cardBrandFilter = FakeCardBrandFilter(disallowedBrands = setOf(CardBrand.MasterCard))
        val cardNumberConfig = cardNumberConfig(
            isCardBrandChoiceEligible = true,
            cardBrandFilter = cardBrandFilter
        )

        val state = cardNumberConfig.determineState(
            brand = CardBrand.MasterCard,
            funding = null,
            number = "55555555", // Length is 8
            numberAllowedDigits = CardBrand.MasterCard.getMaxLengthForCardNumber("55555555")
        )

        // Since number length is less than or equal to 8, it should not return disallowed brand error
        assertThat(state).isInstanceOf<TextFieldStateConstants.Error.Incomplete>()
    }

    @Test
    fun `determineState returns error for disallowed brand with CBC when number length is greater than 8`() {
        val cardBrandFilter = FakeCardBrandFilter(disallowedBrands = setOf(CardBrand.MasterCard))
        val cardNumberConfig = cardNumberConfig(
            isCardBrandChoiceEligible = true,
            cardBrandFilter = cardBrandFilter
        )

        val state = cardNumberConfig.determineState(
            brand = CardBrand.MasterCard,
            funding = null,
            number = "5555555555554444", // Length is greater than 8
            numberAllowedDigits = CardBrand.MasterCard.getMaxLengthForCardNumber("5555555555554444")
        )

        assertThat(state).isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        assertThat(state.getError()?.errorMessage)
            .isEqualTo(StripeR.string.stripe_disallowed_card_brand)
    }

    @Test
    fun `determineState returns valid for allowed brand with CBC`() {
        val cardBrandFilter = FakeCardBrandFilter(disallowedBrands = setOf(CardBrand.MasterCard))
        val cardNumberConfig = cardNumberConfig(
            isCardBrandChoiceEligible = true,
            cardBrandFilter = cardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter
        )

        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            funding = null,
            number = "4242424242424242",
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242")
        )

        assertThat(state).isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    // Card Funding Filtering Tests

    @Test
    fun `determineState returns valid for allowed funding type`() {
        val cardNumberConfig = cardNumberConfigWithFundingFilter(setOf(CardFunding.Credit))

        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            number = "4242424242424242", // Visa debit card
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242"),
            funding = CardFunding.Debit
        )

        assertThat(state).isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    @Test
    fun `determineState returns error for disallowed funding type with 6+ digits`() {
        val cardNumberConfig = cardNumberConfigWithFundingFilter(
            disallowedFundingTypes = setOf(CardFunding.Credit),
            allowedFundingTypesDisplayMessage = StripeR.string.stripe_card_funding_only_debit_prepaid
        )

        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            number = "4242424242424242", // 16 digits
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242"),
            funding = CardFunding.Credit
        )

        assertThat(state).isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        assertThat(state.getError()?.errorMessage)
            .isEqualTo(StripeR.string.stripe_card_funding_only_debit_prepaid)
    }

    @Test
    fun `determineState allows disallowed funding type with less than 6 digits`() {
        val cardNumberConfig = cardNumberConfigWithFundingFilter(setOf(CardFunding.Credit))

        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            number = "42424", // Only 5 digits
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("42424"),
            funding = CardFunding.Credit
        )

        // Should NOT show funding error yet - need 6+ digits
        assertThat(state).isInstanceOf<TextFieldStateConstants.Error.Incomplete>()
    }

    @Test
    fun `determineState returns error for disallowed funding type with exactly 6 digits`() {
        val cardNumberConfig = cardNumberConfigWithFundingFilter(
            disallowedFundingTypes = setOf(CardFunding.Prepaid),
            allowedFundingTypesDisplayMessage = StripeR.string.stripe_card_funding_only_debit_credit
        )

        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            number = "424242", // Exactly 6 digits
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("424242"),
            funding = CardFunding.Prepaid
        )

        // Should show funding error at 6 digits
        assertThat(state).isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        assertThat(state.getError()?.errorMessage)
            .isEqualTo(StripeR.string.stripe_card_funding_only_debit_credit)
    }

    @Test
    fun `determineState returns valid when all funding types are accepted`() {
        val cardNumberConfig = cardNumberConfigWithFundingFilter()

        val state = cardNumberConfig.determineState(
            brand = CardBrand.MasterCard,
            number = "5555555555554444",
            numberAllowedDigits = CardBrand.MasterCard.getMaxLengthForCardNumber("5555555555554444"),
            funding = CardFunding.Credit
        )

        assertThat(state).isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    @Test
    fun `determineState does not prevent input for disallowed funding type`() {
        val cardNumberConfig = cardNumberConfigWithFundingFilter(
            disallowedFundingTypes = setOf(CardFunding.Debit),
            allowedFundingTypesDisplayMessage = StripeR.string.stripe_card_funding_only_debit_credit
        )

        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            number = "4242424242424242",
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242"),
            funding = CardFunding.Debit
        )

        assertThat(state.isFull()).isFalse()
    }

    private fun cardNumberConfigWithFundingFilter(
        disallowedFundingTypes: Set<CardFunding> = emptySet(),
        allowedFundingTypesDisplayMessage: Int? = null
    ): CardNumberConfig {
        val cardFundingFilter = FakeCardFundingFilter(disallowedFundingTypes, allowedFundingTypesDisplayMessage)
        return cardNumberConfig(
            isCardBrandChoiceEligible = false,
            cardBrandFilter = DefaultCardBrandFilter,
            cardFundingFilter = cardFundingFilter
        )
    }

    private fun cardNumberConfig(
        isCardBrandChoiceEligible: Boolean,
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        cardFundingFilter: CardFundingFilter = DefaultCardFundingFilter
    ): CardNumberConfig {
        return CardNumberConfig(
            isCardBrandChoiceEligible = isCardBrandChoiceEligible,
            cardBrandFilter = cardBrandFilter,
            cardFundingFilter = cardFundingFilter
        )
    }
}
