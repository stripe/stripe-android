package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
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
            val cardNumberConfig = CardNumberConfig(
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
            val cardNumberConfig = CardNumberConfig(
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
            val cardNumberConfig = CardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )
            assertThat(
                cardNumberConfig.determineState(
                    brand = CardBrand.Visa,
                    accountRanges = emptyList(),
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
            val cardNumberConfig = CardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )
            val state = cardNumberConfig.determineState(
                brand = CardBrand.Unknown,
                accountRanges = emptyList(),
                number = "0",
                numberAllowedDigits = CardBrand.Unknown.getMaxLengthForCardNumber("0")
            )
            assertThat(state)
                .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
            assertThat(
                state.getValidationMessage()?.message
            ).isEqualTo(StripeR.string.stripe_invalid_card_number)
        }
    }

    @Test
    fun `incomplete number is in incomplete state`() {
        for (isCardBrandChoiceEligible in cardBrandChoiceOptions) {
            val cardNumberConfig = CardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )
            val state = cardNumberConfig.determineState(
                brand = CardBrand.Visa,
                accountRanges = emptyList(),
                number = "12",
                numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("12")
            )
            assertThat(state)
                .isInstanceOf<TextFieldStateConstants.Error.Incomplete>()
            assertThat(
                state.getValidationMessage()?.message
            ).isEqualTo(StripeR.string.stripe_invalid_card_number)
        }
    }

    @Test
    fun `card number is too long`() {
        for (isCardBrandChoiceEligible in cardBrandChoiceOptions) {
            val cardNumberConfig = CardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )
            val state = cardNumberConfig.determineState(
                brand = CardBrand.Visa,
                accountRanges = emptyList(),
                number = "1234567890123456789",
                numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("1234567890123456789")
            )
            assertThat(state)
                .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
            assertThat(
                state.getValidationMessage()?.message
            ).isEqualTo(StripeR.string.stripe_invalid_card_number)
        }
    }

    @Test
    fun `card number has invalid luhn`() {
        for (isCardBrandChoiceEligible in cardBrandChoiceOptions) {
            val cardNumberConfig = CardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )
            val state = cardNumberConfig.determineState(
                brand = CardBrand.Visa,
                accountRanges = emptyList(),
                number = "4242424242424243",
                numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424243")
            )
            assertThat(state)
                .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
            assertThat(
                state.getValidationMessage()?.message
            ).isEqualTo(StripeR.string.stripe_invalid_card_number)
        }
    }

    @Test
    fun `card number is valid`() {
        for (isCardBrandChoiceEligible in cardBrandChoiceOptions) {
            val cardNumberConfig = CardNumberConfig(
                isCardBrandChoiceEligible = isCardBrandChoiceEligible,
                cardBrandFilter = DefaultCardBrandFilter
            )
            val state = cardNumberConfig.determineState(
                brand = CardBrand.Visa,
                accountRanges = emptyList(),
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
        val cardNumberConfig = CardNumberConfig(
            isCardBrandChoiceEligible = false,
            cardBrandFilter = cardBrandFilter
        )

        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            accountRanges = emptyList(),
            number = "4242424242424242",
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242")
        )

        assertThat(state).isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    @Test
    fun `determineState returns error for disallowed brand without CBC`() {
        val cardBrandFilter = FakeCardBrandFilter(disallowedBrands = setOf(CardBrand.MasterCard))
        val cardNumberConfig = CardNumberConfig(
            isCardBrandChoiceEligible = false,
            cardBrandFilter = cardBrandFilter
        )

        val state = cardNumberConfig.determineState(
            brand = CardBrand.MasterCard,
            accountRanges = emptyList(),
            number = "5555555555554444",
            numberAllowedDigits = CardBrand.MasterCard.getMaxLengthForCardNumber("5555555555554444")
        )

        assertThat(state).isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        assertThat(state.getValidationMessage()?.message)
            .isEqualTo(StripeR.string.stripe_disallowed_card_brand)
    }

    @Test
    fun `determineState allows disallowed brand with CBC when number length is less than or equal to 8`() {
        val cardBrandFilter = FakeCardBrandFilter(disallowedBrands = setOf(CardBrand.MasterCard))
        val cardNumberConfig = CardNumberConfig(
            isCardBrandChoiceEligible = true,
            cardBrandFilter = cardBrandFilter
        )

        val state = cardNumberConfig.determineState(
            brand = CardBrand.MasterCard,
            accountRanges = emptyList(),
            number = "55555555", // Length is 8
            numberAllowedDigits = CardBrand.MasterCard.getMaxLengthForCardNumber("55555555")
        )

        // Since number length is less than or equal to 8, it should not return disallowed brand error
        assertThat(state).isInstanceOf<TextFieldStateConstants.Error.Incomplete>()
    }

    @Test
    fun `determineState returns error for disallowed brand with CBC when number length is greater than 8`() {
        val cardBrandFilter = FakeCardBrandFilter(disallowedBrands = setOf(CardBrand.MasterCard))
        val cardNumberConfig = CardNumberConfig(
            isCardBrandChoiceEligible = true,
            cardBrandFilter = cardBrandFilter
        )

        val state = cardNumberConfig.determineState(
            brand = CardBrand.MasterCard,
            accountRanges = emptyList(),
            number = "5555555555554444", // Length is greater than 8
            numberAllowedDigits = CardBrand.MasterCard.getMaxLengthForCardNumber("5555555555554444")
        )

        assertThat(state).isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        assertThat(state.getValidationMessage()?.message)
            .isEqualTo(StripeR.string.stripe_disallowed_card_brand)
    }

    @Test
    fun `determineState returns valid for allowed brand with CBC`() {
        val cardBrandFilter = FakeCardBrandFilter(disallowedBrands = setOf(CardBrand.MasterCard))
        val cardNumberConfig = CardNumberConfig(
            true,
            cardBrandFilter,
            DefaultCardFundingFilter
        )

        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            accountRanges = emptyList(),
            number = "4242424242424242",
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242")
        )

        assertThat(state).isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    @Test
    fun `determineState does not show funding error when card has less than 6 digits`() {
        val cardFundingFilter = FakeCardFundingFilter(
            disallowedFundingTypes = setOf(CardFunding.Debit),
            messageResId = StripeR.string.stripe_card_funding_only_credit
        )
        val cardNumberConfig = CardNumberConfig(
            false,
            DefaultCardBrandFilter,
            cardFundingFilter
        )

        // 5 digits, with debit funding (which is disallowed)
        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            accountRanges = listOf(CardFunding.Debit),
            number = "42424",
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("42424")
        )

        // Should show incomplete error, not funding error
        assertThat(state).isInstanceOf<TextFieldStateConstants.Error.Incomplete>()
        assertThat(state.getValidationMessage()?.message)
            .isEqualTo(StripeR.string.stripe_invalid_card_number)
    }

    @Test
    fun `determineState shows funding warning when incomplete card has 6+ digits with disallowed funding`() {
        val cardFundingFilter = FakeCardFundingFilter(
            disallowedFundingTypes = setOf(CardFunding.Debit),
            messageResId = StripeR.string.stripe_card_funding_only_credit
        )
        val cardNumberConfig = CardNumberConfig(
            false,
            DefaultCardBrandFilter,
            cardFundingFilter
        )

        // 10 digits (incomplete), with debit funding (which is disallowed)
        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            accountRanges = listOf(CardFunding.Debit),
            number = "4242424242",
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242")
        )

        // Should show invalid state with warning message, allowing more input
        assertThat(state).isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        assertThat(state.getValidationMessage()?.message)
            .isEqualTo(StripeR.string.stripe_card_funding_only_credit)
        assertThat(state.isFull()).isFalse()
    }

    @Test
    fun `determineState shows funding warning when complete card has disallowed funding`() {
        val cardFundingFilter = FakeCardFundingFilter(
            disallowedFundingTypes = setOf(CardFunding.Debit),
            messageResId = StripeR.string.stripe_card_funding_only_credit
        )
        val cardNumberConfig = CardNumberConfig(
            false,
            DefaultCardBrandFilter,
            cardFundingFilter
        )

        // Complete valid card with debit funding (which is disallowed)
        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            accountRanges = listOf(CardFunding.Debit),
            number = "4242424242424242",
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242")
        )

        // Should show valid state with warning message
        assertThat(state).isInstanceOf<TextFieldStateConstants.Valid.Full>()
        assertThat(state.getValidationMessage()?.message)
            .isEqualTo(StripeR.string.stripe_card_funding_only_credit)
    }

    @Test
    fun `determineState returns valid without warning for allowed funding type`() {
        val cardFundingFilter = FakeCardFundingFilter(
            disallowedFundingTypes = setOf(CardFunding.Debit),
            messageResId = StripeR.string.stripe_card_funding_only_credit
        )
        val cardNumberConfig = CardNumberConfig(
            false,
            DefaultCardBrandFilter,
            cardFundingFilter
        )

        // Complete valid card with credit funding (which is allowed)
        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            accountRanges = listOf(CardFunding.Credit),
            number = "4242424242424242",
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242")
        )

        // Should show valid state without warning
        assertThat(state).isInstanceOf<TextFieldStateConstants.Valid.Full>()
        assertThat(state.getValidationMessage()).isNull()
    }

    @Test
    fun `determineState returns valid when funding list is empty`() {
        val cardFundingFilter = FakeCardFundingFilter(
            disallowedFundingTypes = setOf(CardFunding.Debit),
            messageResId = StripeR.string.stripe_card_funding_only_credit
        )
        val cardNumberConfig = CardNumberConfig(
            false,
            DefaultCardBrandFilter,
            cardFundingFilter
        )

        // Complete valid card with empty funding list (funding not yet determined)
        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            accountRanges = emptyList(),
            number = "4242424242424242",
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242")
        )

        // Should show valid state without warning
        assertThat(state).isInstanceOf<TextFieldStateConstants.Valid.Full>()
        assertThat(state.getValidationMessage()).isNull()
    }

    @Test
    fun `determineState returns valid without warning when any funding type is accepted`() {
        val cardFundingFilter = FakeCardFundingFilter(
            disallowedFundingTypes = setOf(CardFunding.Debit),
            messageResId = StripeR.string.stripe_card_funding_only_credit
        )
        val cardNumberConfig = CardNumberConfig(
            false,
            DefaultCardBrandFilter,
            cardFundingFilter
        )

        // Complete valid card with both debit (disallowed) and credit (allowed) funding
        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            accountRanges = listOf(CardFunding.Debit, CardFunding.Credit),
            number = "4242424242424242",
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242")
        )

        // Should show valid state without warning because credit is accepted
        assertThat(state).isInstanceOf<TextFieldStateConstants.Valid.Full>()
        assertThat(state.getValidationMessage()).isNull()
    }

    @Test
    fun `determineState does not show funding warning when filter has no message`() {
        val cardFundingFilter = FakeCardFundingFilter(
            disallowedFundingTypes = setOf(CardFunding.Debit),
            messageResId = null // No message configured
        )
        val cardNumberConfig = CardNumberConfig(
            false,
            DefaultCardBrandFilter,
            cardFundingFilter
        )

        // Complete valid card with debit funding (which is disallowed but has no message)
        val state = cardNumberConfig.determineState(
            brand = CardBrand.Visa,
            accountRanges = listOf(CardFunding.Debit),
            number = "4242424242424242",
            numberAllowedDigits = CardBrand.Visa.getMaxLengthForCardNumber("4242424242424242")
        )

        // Should show valid state without warning (because no message is configured)
        assertThat(state).isInstanceOf<TextFieldStateConstants.Valid.Full>()
        assertThat(state.getValidationMessage()).isNull()
    }
}
