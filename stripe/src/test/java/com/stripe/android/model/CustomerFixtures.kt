package com.stripe.android.model

import com.stripe.android.model.parsers.CustomerJsonParser
import org.json.JSONObject

internal object CustomerFixtures {

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

    val CUSTOMER_WITH_SHIPPING = requireNotNull(CustomerJsonParser().parse(JSONObject(
        """
        {
            "id": "cus_AQsHpvKfKwJDrF",
            "object": "customer",
            "default_source": "abc123",
            "shipping": {
                "address": {
                    "city": "San Francisco",
                    "country": "US",
                    "line1": "185 Berry St",
                    "line2": null,
                    "postal_code": "94087",
                    "state": "CA"
                },
                "name": "Kathy",
                "phone": "1234567890"
            },
            "sources": {
                "object": "list",
                "data": [
        
                ],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/customers/cus_AQsHpvKfKwJDrF/sources"
            }
        }
        """.trimIndent()
    )))

    val CUSTOMER = requireNotNull(CustomerJsonParser().parse(CUSTOMER_JSON))

    val OTHER_CUSTOMER = requireNotNull(CustomerJsonParser().parse(JSONObject(
        """
        {
            "id": "cus_ABC123",
            "object": "customer",
            "default_source": "def456",
            "sources": {
                "object": "list",
                "data": [
        
                ],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/customers/cus_ABC123/sources"
            }
        }
        """.trimIndent()
    )))

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
