package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.SamsungPayTokenParams
import org.junit.Test

class SamsungPayTokenParamsTest {
    @Test
    fun `payment credential maps to Samsung Pay wallet token parameters`() {
        val params = SamsungPayTokenParams("{\"method\":\"3DS\"}")

        assertThat(params.toParamMap()).isEqualTo(
            mapOf(
                "card" to mapOf(
                    "wallet" to mapOf(
                        "type" to "samsung_pay",
                        "samsung_pay" to mapOf(
                            "token" to "{\"method\":\"3DS\"}",
                        ),
                    ),
                ),
            ),
        )
        assertThat(params.attribution).containsExactly("samsung_pay")
    }
}
