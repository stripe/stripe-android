package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class CardParamsTest {

    @Test
    fun `toParamMap() should create expected map`() {
        assertThat(CardParamsFixtures.DEFAULT.toParamMap())
            .isEqualTo(
                mapOf(
                    "card" to
                        mapOf(
                            "number" to "4242424242424242",
                            "exp_month" to 12,
                            "exp_year" to 2045,
                            "cvc" to "123",
                            "name" to "Jenny Rosen",
                            "currency" to "usd",
                            "address_line1" to "123 Market St",
                            "address_line2" to "#345",
                            "address_city" to "San Francisco",
                            "address_state" to "CA",
                            "address_zip" to "94107",
                            "address_country" to "US",
                            "metadata" to mapOf("fruit" to "orange")
                        )
                )
            )
    }
}
