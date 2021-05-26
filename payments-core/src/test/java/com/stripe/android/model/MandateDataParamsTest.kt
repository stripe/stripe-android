package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MandateDataParamsTest {

    @Test
    fun toParamMap_shouldCreateExpectedObject() {
        val expectedParams = mapOf(
            "customer_acceptance" to mapOf(
                "type" to "online",
                "online" to mapOf(
                    "ip_address" to "127.0.0.1",
                    "user_agent" to "my_user_agent"
                )
            )
        )

        assertEquals(
            expectedParams,
            MandateDataParamsFixtures.DEFAULT.toParamMap()
        )
    }

    @Test
    fun toParamMap_whenInferFromClient_shouldCreateExpectedObject() {
        val actualParams = MandateDataParams(
            MandateDataParams.Type.Online.DEFAULT
        ).toParamMap()

        val expectedParams = mapOf(
            "customer_acceptance" to mapOf(
                "type" to "online",
                "online" to mapOf(
                    "infer_from_client" to true
                )
            )
        )

        assertEquals(expectedParams, actualParams)
    }
}
