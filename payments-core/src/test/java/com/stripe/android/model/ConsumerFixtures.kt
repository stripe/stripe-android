package com.stripe.android.model

import com.stripe.android.model.parsers.ConsumerSessionLookupJsonParser
import org.json.JSONObject

object ConsumerFixtures {

    val NO_EXISTING_CONSUMER_JSON = JSONObject(
        """
            {
              "consumer_session": null,
              "cookies_operations": null,
              "error_message": "No consumer found for the given email address.",
              "exists": false
            }
        """.trimIndent()
    )
    val NO_EXISTING_CONSUMER = ConsumerSessionLookupJsonParser().parse(NO_EXISTING_CONSUMER_JSON)

    val EXISTING_CONSUMER_JSON = JSONObject(
        """
            {
              "consumer_session": {
                "client_secret": "secret",
                "email_address": "email@example.com",
                "redacted_phone_number": "+1********68",
                "support_payment_details_types": [
                  "CARD"
                ],
                "verification_sessions": []
              },
              "cookies_operations": {
                "operations": []
              },
              "error_message": null,
              "exists": true
            }
        """.trimIndent()
    )
    val EXISTING_CONSUMER = ConsumerSessionLookupJsonParser().parse(EXISTING_CONSUMER_JSON)
}
