package com.stripe.android.model

import com.stripe.android.model.parsers.CustomerJsonParser
import org.json.JSONObject

object CustomerFixtures {
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

    val CUSTOMER = requireNotNull(CustomerJsonParser().parse(CUSTOMER_JSON))
}
