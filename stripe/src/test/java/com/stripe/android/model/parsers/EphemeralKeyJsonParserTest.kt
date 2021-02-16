package com.stripe.android.model.parsers

import com.stripe.android.EphemeralKey
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals

class EphemeralKeyJsonParserTest {

    @Test
    fun parse() {
        val actual = EphemeralKeyJsonParser().parse(
            JSONObject(
                """
            {
                "id": "ephkey_123",
                "object": "ephemeral_key",
                "secret": "ek_test_123",
                "created": 1483575790,
                "livemode": false,
                "expires": 1483579790,
                "associated_objects": [{
                    "type": "customer",
                    "id": "cus_123"
                }]
            }
                """.trimIndent()
            )
        )

        val expected = EphemeralKey(
            objectId = "cus_123",
            id = "ephkey_123",
            secret = "ek_test_123",
            isLiveMode = false,
            created = 1483575790L,
            expires = 1483579790L,
            type = "customer",
            objectType = "ephemeral_key"
        )

        assertEquals(expected, actual)
    }
}
