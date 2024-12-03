package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConsumerPaymentDetailsTest {

    @Test
    fun testGetAddressFromMap() {
        val params = mapOf(
            "billing_details" to mapOf(
                "address" to mapOf(
                    "country" to "CA",
                    "postal_code" to "M1B2K7"
                )
            )
        )

        val result = ConsumerPaymentDetails.Card.getAddressFromMap(params)

        assertThat(result).isEqualTo(
            "billing_address" to mapOf(
                "country_code" to "CA",
                "postal_code" to "M1B2K7"
            )
        )
    }
}
