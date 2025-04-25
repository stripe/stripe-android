package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConsumerPaymentDetailsUpdateParamsTest {

    @Test
    fun `toParamMap should include is_default when provided`() {
        val params = ConsumerPaymentDetailsUpdateParams(
            id = "id",
            isDefault = true
        ).toParamMap()

        assertThat(params["is_default"]).isEqualTo(true)
    }

    @Test
    fun `toParamMap should include card details when provided`() {
        val cardDetails = mapOf(
            "card" to mapOf(
                "exp_month" to 12,
                "exp_year" to 2030,
                "networks" to mapOf("preferred" to "visa")
            )
        )
        val params = ConsumerPaymentDetailsUpdateParams(
            id = "id",
            cardPaymentMethodCreateParamsMap = cardDetails
        ).toParamMap()

        assertThat(params["exp_month"]).isEqualTo(12)
        assertThat(params["exp_year"]).isEqualTo(2030)
        assertThat(params["preferred_network"]).isEqualTo("visa")
    }

    @Test
    fun `toParamMap should include billing address when provided`() {
        val cardDetails = mapOf(
            "billing_details" to mapOf(
                "address" to mapOf(
                    "country" to "CA",
                    "postal_code" to "M1B2K7"
                )
            )
        )
        val params = ConsumerPaymentDetailsUpdateParams(
            id = "id",
            cardPaymentMethodCreateParamsMap = cardDetails
        ).toParamMap()
        val address = params["billing_address"] as Map<*, *>

        assertThat(address["country_code"]).isEqualTo("CA")
        assertThat(address["postal_code"]).isEqualTo("M1B2K7")
    }

    @Test
    fun `toParamMap should return empty map when no optional fields are provided`() {
        val params = ConsumerPaymentDetailsUpdateParams(
            id = "id"
        ).toParamMap()

        assertThat(params).isEmpty()
    }
}