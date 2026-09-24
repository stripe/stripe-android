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
                "tax_context",
                JSONObject()
                    .put("automatic_tax_enabled", true)
                    .put("automatic_tax_address_source", "session.billing")
            )
            json.put(
                "tax_meta",
                JSONObject()
                    .put("computation_type", "automatic")
                    .put("status", "requires_location_inputs")
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
