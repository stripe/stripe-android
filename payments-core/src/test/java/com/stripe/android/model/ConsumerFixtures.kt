package com.stripe.android.model

import com.stripe.android.model.parsers.ConsumerSessionJsonParser
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

    val CONSUMER_VERIFICATION_STARTED_JSON = JSONObject(
        """
            {
              "consumer_session": {
                "client_secret": "12oBEhVjc21yKkFYNnhMVTlXbXdBQUFJRmEaJDNmZDE1MjA5LTM1YjctND",
                "email_address": "test@stripe.com",
                "redacted_phone_number": "+1********56",
                "support_payment_details_types": [
                  "CARD"
                ],
                "verification_sessions": [
                  {
                    "state": "STARTED",
                    "type": "SMS"
                  }
                ]
              },
              "cookies_operations": {
                "operations": [
            
                ]
              }
            }
        """.trimIndent()
    )
    val CONSUMER_VERIFICATION_STARTED =
        ConsumerSessionJsonParser().parse(CONSUMER_VERIFICATION_STARTED_JSON)

    val CONSUMER_VERIFIED_JSON = JSONObject(
        """
            {
              "consumer_session": {
                "client_secret": "12oBEhVjc21yKkFYNnhMVTlXbXdBQUFJRmEaJDUzNTFkNjNhLTZkNGMtND",
                "email_address": "test@stripe.com",
                "redacted_phone_number": "+1********56",
                "support_payment_details_types": [
                  "CARD"
                ],
                "verification_sessions": [
                  {
                    "state": "VERIFIED",
                    "type": "SMS"
                  }
                ]
              },
              "cookies_operations": {
                "operations": [
                  {
                    "operation": "ADD",
                    "verification_session_client_secret": "12oBEhVjc21yKkFYNnhMVTlXbXdBQUFJRmEaJGFkYmQ2YmQ3LWZiYmYtND"
                  }
                ]
              }
            }
        """.trimIndent()
    )
    val CONSUMER_VERIFIED = ConsumerSessionJsonParser().parse(CONSUMER_VERIFIED_JSON)

    val CONSUMER_SIGNUP_STARTED_JSON = JSONObject(
        """
            {
              "consumer_session": {
                "client_secret": "12oBEhVjc21yKkFYNmNWT0JmaFFBQUFLUXcaJDk5OGFjYTFlLTkxMWYtND",
                "email_address": "test@stripe.com",
                "redacted_phone_number": "+1********23",
                "support_payment_details_types": [
                  "CARD"
                ],
                "verification_sessions": [
                  {
                    "state": "STARTED",
                    "type": "SIGNUP"
                  }
                ]
              }
            }
        """.trimIndent()
    )
    val CONSUMER_SIGNUP_STARTED = ConsumerSessionJsonParser().parse(CONSUMER_SIGNUP_STARTED_JSON)
}
