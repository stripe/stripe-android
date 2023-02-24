package com.stripe.android

import org.json.JSONObject

object ConsumerFixtures {

    val NO_EXISTING_CONSUMER_JSON = JSONObject(
        """
            {
              "consumer_session": null,
              "error_message": "No consumer found for the given email address.",
              "exists": false
            }
        """.trimIndent()
    )

    val EXISTING_CONSUMER_JSON = JSONObject(
        """
            {
              "auth_session_client_secret": null,
              "consumer_session": {
                "client_secret": "secret",
                "email_address": "email@example.com",
                "redacted_phone_number": "+1********68",
                "support_payment_details_types": [
                  "CARD"
                ],
                "verification_sessions": []
              },
              "error_message": null,
              "exists": true
            }
        """.trimIndent()
    )

    val CONSUMER_VERIFICATION_STARTED_JSON = JSONObject(
        """
            {
              "auth_session_client_secret": "21yKkFYNnhMVTlXbXdBQUFJRmEaJDNmZDE1",
              "publishable_key": "asdfg123",
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
              }
            }
        """.trimIndent()
    )

    val CONSUMER_VERIFIED_JSON = JSONObject(
        """
            {
              "auth_session_client_secret": null,
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
              }
            }
        """.trimIndent()
    )

    val CONSUMER_SIGNUP_STARTED_JSON = JSONObject(
        """
            {
              "auth_session_client_secret": null,
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
}
