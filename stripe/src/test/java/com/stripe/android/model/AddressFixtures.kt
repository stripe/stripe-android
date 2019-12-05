package com.stripe.android.model

import com.stripe.android.model.parsers.AddressJsonParser
import org.json.JSONObject

internal object AddressFixtures {
    val ADDRESS = AddressJsonParser().parse(JSONObject(
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
    ))
}
