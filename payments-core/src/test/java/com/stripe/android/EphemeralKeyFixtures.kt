package com.stripe.android

import com.stripe.android.model.parsers.EphemeralKeyJsonParser
import org.json.JSONObject

internal object EphemeralKeyFixtures {
    val FIRST_JSON = JSONObject(
        """
        {
            "id": "ephkey_123",
            "object": "ephemeral_key",
            "secret": "ek_test_123",
            "created": 1501179335,
            "livemode": false,
            "expires": 1501199335,
            "associated_objects": [{
                "type": "customer",
                "id": "cus_AQsHpvKfKwJDrF"
            }]
        }
        """.trimIndent()
    )

    val SECOND_JSON = JSONObject(
        """
        {
            "id": "ephkey_ABC",
            "object": "ephemeral_key",
            "secret": "ek_test_456",
            "created": 1601189335,
            "livemode": false,
            "expires": 1601199335,
            "associated_objects": [{
                "type": "customer",
                "id": "cus_abc123"
            }]
        }
        """.trimIndent()
    )

    val FIRST = EphemeralKeyJsonParser().parse(FIRST_JSON)
    val SECOND = EphemeralKeyJsonParser().parse(SECOND_JSON)

    fun create(
        expires: Long
    ): EphemeralKey {
        return FIRST.copy(
            expires = expires
        )
    }
}
