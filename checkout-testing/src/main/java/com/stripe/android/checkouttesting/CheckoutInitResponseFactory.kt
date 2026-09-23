package com.stripe.android.checkouttesting

import com.stripe.android.networktesting.testBodyFromFile
import okhttp3.mockwebserver.MockResponse
import org.json.JSONArray
import org.json.JSONObject

object CheckoutInitResponseFactory {
    fun create(response: MockResponse) {
        create(response) {}
    }

    fun createWithRequiredShippingAddress(response: MockResponse) {
        create(response) { json ->
            json.put(
                "shipping_address_collection",
                JSONObject().put("allowed_countries", JSONArray(listOf("US", "CA")))
            )
        }
    }

    private fun create(
        response: MockResponse,
        modify: (JSONObject) -> Unit,
    ) {
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("customer_email", "checkout@example.com")
            json.put("account_settings", JSONObject().put("country", "US"))
            modify(json)
        }
    }
}
