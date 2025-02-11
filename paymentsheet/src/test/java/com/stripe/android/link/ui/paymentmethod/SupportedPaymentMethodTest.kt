package com.stripe.android.link.ui.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import org.junit.Test

class SupportedPaymentMethodTest {

    @Test
    fun `Card PaymentDetails params are converted correctly from PaymentMethodCreateParams`() {
        val paymentMethodCreateParams = mapOf(
            "type" to "card",
            "card" to mapOf(
                "number" to "5555555555554444",
                "exp_month" to "12",
                "exp_year" to "2050",
                "cvc" to "123"
            ),
            "billing_details" to mapOf(
                "address" to mapOf(
                    "country" to "US",
                    "postal_code" to "12345"
                )
            )
        )

        assertThat(
            ConsumerPaymentDetailsCreateParams.Card(
                cardPaymentMethodCreateParamsMap = paymentMethodCreateParams,
                email = "email@test.com",
                active = false,
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "type" to "card",
                "billing_email_address" to "email@test.com",
                "card" to mapOf(
                    "number" to "5555555555554444",
                    "exp_month" to "12",
                    "exp_year" to "2050"
                ),
                "billing_address" to mapOf(
                    "country_code" to "US",
                    "postal_code" to "12345"
                ),
                "active" to false,
            )
        )
    }
}
