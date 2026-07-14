package com.stripe.android.common.nfcscan.scanner

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.wallet.RejectCardBrands
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import org.junit.Test

internal class DefaultNfcCardValidatorTest {
    @Test
    fun `validate returns Validated when card brand is accepted and expiry is valid`() = runScenario {
        val result = validator.validate(
            ScannedCardData(
                cardNumber = "4242424242424242",
                expirationMonth = 12,
                expirationYear = 2030,
            ),
        )

        assertThat(result).isEqualTo(NfcCardValidator.Result.Validated)
    }

    @Test
    fun `validate returns Invalid when card brand is not accepted`() = runScenario(
        cardBrandFilter = RejectCardBrands(CardBrand.Visa),
    ) {
        val result = validator.validate(
            ScannedCardData(
                cardNumber = "4242424242424242",
                expirationMonth = 12,
                expirationYear = 2030,
            ),
        )

        assertThat(result).isEqualTo(
            NfcCardValidator.Result.Invalid(
                userMessage = R.string.stripe_nfc_scan_unsupported_card.resolvableString,
            ),
        )
    }

    @Test
    fun `validate returns Invalid when card is expired`() = runScenario {
        val result = validator.validate(
            ScannedCardData(
                cardNumber = "4242424242424242",
                expirationMonth = 1,
                expirationYear = 2020,
            ),
        )

        assertThat(result).isEqualTo(
            NfcCardValidator.Result.Invalid(
                userMessage = R.string.stripe_nfc_scan_error_expired_card.resolvableString,
            ),
        )
    }

    private fun runScenario(
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        block: Scenario.() -> Unit = {},
    ) {
        val validator = DefaultNfcCardValidator(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                cardBrandFilter = cardBrandFilter,
            ),
        )

        Scenario(validator = validator).block()
    }

    private class Scenario(
        val validator: DefaultNfcCardValidator,
    )
}
