package com.stripe.android.model

import com.stripe.android.model.parsers.AddressJsonParser
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

    val ADDRESS = AddressJsonParser().parse(ADDRESS_JSON)
}
