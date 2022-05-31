package com.stripe.android.model

import com.google.common.truth.Truth
import org.junit.Test

class ConsumerPaymentDetailsUpdateParamsTest {

    @Test
    fun updateCard_withPaymentMethodCreateParams_generatesCorrectParameters() {
        val id = "payment_details_id"
        val paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
            type = PaymentMethod.Type.Card,
            overrideParamMap = mapOf(
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
            productUsage = emptySet()
        )

        Truth.assertThat(
            ConsumerPaymentDetailsUpdateParams.Card(
                id = id,
                cardPaymentMethodCreateParams = paymentMethodCreateParams
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "exp_month" to "12",
                "exp_year" to "2050",
                "billing_address" to mapOf(
                    "country_code" to "US",
                    "postal_code" to "12345"
                )
            )
        )
    }

    @Test
    fun updateCard_withDefaultValue_generatesCorrectParameters() {
        val id = "payment_details_id"

        Truth.assertThat(
            ConsumerPaymentDetailsUpdateParams.Card(
                id = id,
                isDefault = true
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "is_default" to true
            )
        )
    }

    @Test
    fun updateCard_withAllParams_generatesCorrectParameters() {
        val id = "payment_details_id"
        val paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
            type = PaymentMethod.Type.Card,
            overrideParamMap = mapOf(
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
            productUsage = emptySet()
        )

        Truth.assertThat(
            ConsumerPaymentDetailsUpdateParams.Card(
                id = id,
                cardPaymentMethodCreateParams = paymentMethodCreateParams,
                isDefault = false
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "is_default" to false,
                "exp_month" to "12",
                "exp_year" to "2050",
                "billing_address" to mapOf(
                    "country_code" to "US",
                    "postal_code" to "12345"
                )
            )
        )
    }
}
