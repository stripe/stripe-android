package com.stripe.android.model

import org.json.JSONObject

internal object CustomerFixtures {

    @JvmField
    val CUSTOMER_JSON = JSONObject(
        """
        {
            "id": "cus_AQsHpvKfKwJDrF",
            "object": "customer",
            "default_source": "abc123",
            "sources": {
                "object": "list",
                "data": [],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/customers/cus_AQsHpvKfKwJDrF/sources"
            }
        }
        """.trimIndent()
    )

    @JvmField
    val CUSTOMER = Customer.fromJson(CUSTOMER_JSON)!!

    @JvmField
    val EPHEMERAL_KEY_FIRST = JSONObject(
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

    @JvmField
    val EPHEMERAL_KEY_SECOND = JSONObject(
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
}
