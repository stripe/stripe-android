package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodOptions
import com.stripe.android.model.PaymentMethodOptionsFixtures
import kotlin.test.Test

class PaymentMethodOptionsJsonParserTest {

    @Test
    fun `parse payment intent payment method options returns expected options`() {
        assertThat(
            PaymentMethodOptionsJsonParser().parse(
                PaymentMethodOptionsFixtures.PI_CARD_PAYMENT_METHOD_OPTIONS_JSON
            )
        ).isEqualTo(
            PaymentMethodOptions(
                setupFutureUsage = PaymentMethodOptions.SetupFutureUsage.OnSession,
            )
        )
        assertThat(
            PaymentMethodOptionsJsonParser().parse(
                PaymentMethodOptionsFixtures.PI_US_BANK_ACCOUNT_PAYMENT_METHOD_OPTIONS_JSON
            )
        ).isEqualTo(
            PaymentMethodOptions(
                setupFutureUsage = PaymentMethodOptions.SetupFutureUsage.OnSession,
                verificationMethod = PaymentMethodOptions.VerificationMethod.Microdeposits
            )
        )
    }

    @Test
    fun `parse setup intent payment method options returns expected options`() {
        assertThat(
            PaymentMethodOptionsJsonParser().parse(
                PaymentMethodOptionsFixtures.SI_CARD_PAYMENT_METHOD_OPTIONS_JSON
            )
        ).isEqualTo(
            PaymentMethodOptions()
        )

        assertThat(
            PaymentMethodOptionsJsonParser().parse(
                PaymentMethodOptionsFixtures.SI_US_BANK_ACCOUNT_PAYMENT_METHOD_OPTIONS_JSON
            )
        ).isEqualTo(
            PaymentMethodOptions(
                verificationMethod = PaymentMethodOptions.VerificationMethod.Automatic
            )
        )
    }
}
