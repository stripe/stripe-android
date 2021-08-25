package com.stripe.android.model

import com.stripe.android.model.parsers.AddressJsonParser
import org.json.JSONObject

object AddressFixtures {
    @JvmField
    val ADDRESS: Address = AddressJsonParser().parse(
        JSONObject(
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
    )
}
