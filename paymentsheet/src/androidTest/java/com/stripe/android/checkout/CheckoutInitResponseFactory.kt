package com.stripe.android.checkout

import com.stripe.android.networktesting.testBodyFromFile
import okhttp3.mockwebserver.MockResponse
import org.json.JSONObject

internal object CheckoutInitResponseFactory {
    fun create(response: MockResponse) {
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("customer_email", "checkout@example.com")
            json.put("account_settings", JSONObject().put("country", "US"))
        }
    }
}
