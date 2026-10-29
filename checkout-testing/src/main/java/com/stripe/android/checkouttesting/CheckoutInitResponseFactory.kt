package com.stripe.android.checkouttesting

import com.stripe.android.networktesting.testBodyFromFile
import okhttp3.mockwebserver.MockResponse
import org.json.JSONObject

object CheckoutInitResponseFactory {
    fun create(response: MockResponse) {
        create(response) {}
    }

    fun createWithRequiredBillingAddress(response: MockResponse) {
        create(response) { json ->
            json.put("billing_address_collection", "required")
        }
    }

    fun createWithRequiredBillingAddressForAutomaticTax(response: MockResponse) {
        create(response) { json ->
            json.put("billing_address_collection", "required")
            json.put(
                "automatic_tax",
                JSONObject()
                    .put("enabled", true)
                    .put("status", "ready")
                    .put("address_source", "billing")
            )
            json.getJSONObject("automatic_tax").put("status", "requires_address")
        }
    }

    private fun create(
        response: MockResponse,
        modify: (JSONObject) -> Unit,
    ) {
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("customer_email", "checkout@example.com")
            modify(json)
        }
    }
}
