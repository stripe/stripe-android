package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodOptionsFixtures
import com.stripe.android.model.PaymentMethodOptionsMap
import kotlin.test.Test

class PaymentMethodOptionsMapJsonParserTest {

    @Test
    fun `parse payment intent payment method options returns expected options`() {
        assertThat(
            PaymentMethodOptionsJsonParser().parse(PaymentMethodOptionsFixtures.PI_PAYMENT_METHOD_OPTIONS_JSON)
        ).isEqualTo(
            PaymentMethodOptionsMap(
                options = mapOf(
                    "card" to PaymentMethodOptionsMap.Options(
                        setupFutureUsage = PaymentMethodOptionsMap.SetupFutureUsage.OnSession,
                    ),
                    "us_bank_account" to PaymentMethodOptionsMap.Options(
                        setupFutureUsage = PaymentMethodOptionsMap.SetupFutureUsage.OnSession,
                        verificationMethod = PaymentMethodOptionsMap.VerificationMethod.Microdeposits
                    )
                )
            )
        )
    }

    @Test
    fun `parse setup intent payment method options returns expected options`() {
        assertThat(
            PaymentMethodOptionsJsonParser().parse(PaymentMethodOptionsFixtures.SI_PAYMENT_METHOD_OPTIONS_JSON)
        ).isEqualTo(
            PaymentMethodOptionsMap(
                options = mapOf(
                    "card" to PaymentMethodOptionsMap.Options(),
                    "us_bank_account" to PaymentMethodOptionsMap.Options(
                        verificationMethod = PaymentMethodOptionsMap.VerificationMethod.Automatic
                    )
                )
            )
        )
    }
}
