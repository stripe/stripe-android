package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConsumerPaymentDetailsCreateParamsTest {

    @Test
    fun createCardParams_generatesCorrectParameters() {
        assertThat(
            ConsumerPaymentDetailsCreateParams.Card(
                mapOf(
                    "ignored" to "none",
                    "card" to mapOf(
                        "number" to "123",
                        "cvc" to "321",
                        "brand" to "visa",
                        "exp_month" to "12",
                        "exp_year" to "2050"
                    ),
                    "billing_details" to mapOf<String, Any>(
                        "address" to mapOf(
                            "country" to "US",
                            "postal_code" to "12345",
                            "extra" to "1"
                        )
                    )
                ),
                "email@stripe.com"
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "type" to "card",
                "billing_email_address" to "email@stripe.com",
                "card" to mapOf(
                    "number" to "123",
                    "exp_month" to "12",
                    "exp_year" to "2050"
                ),
                "billing_address" to mapOf(
                    "country_code" to "US",
                    "postal_code" to "12345"
                )
            )
        )
    }
}
