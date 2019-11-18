package com.stripe.android.model

import org.json.JSONObject

internal object AddressFixtures {
    val ADDRESS_JSON = JSONObject(
        """
        {
            "city": "San Francisco",
            "country": "US",
            "line1": "123 Market St",
            "line2": "#345",
            "postal_code": "94107",
            "state": "CA"
        }
        """.trimIndent()
    )

    val ADDRESS = requireNotNull(Address.fromJson(ADDRESS_JSON))
}
