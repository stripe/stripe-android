package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class SamsungPayTokenParamsTest {
    @Test
    fun toParamMap_createsExpectedMap() {
        val token = "{\"method\":\"3DS\"}"

        assertThat(SamsungPayTokenParams(token).toParamMap())
            .isEqualTo(
                mapOf(
                    "card" to mapOf(
                        "wallet" to mapOf(
                            "type" to "samsung_pay",
                            "samsung_pay" to mapOf(
                                "token" to token
                            )
                        )
                    )
                )
            )
    }
}
