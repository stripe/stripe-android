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
              "publishable_key": "asdfg123",
              "consumer_session": {
                "client_secret": "secret",
                "email_address": "email@example.com",
                "redacted_phone_number": "+1********68",
                "redacted_formatted_phone_number": "(***) *** **68",
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
                "redacted_formatted_phone_number": "(***) *** **56",
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
                "redacted_formatted_phone_number": "(***) *** **56",
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
                "redacted_formatted_phone_number": "(***) *** **23",
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

    val CONSUMER_SINGLE_CARD_PAYMENT_DETAILS_JSON = JSONObject(
        """
            {
              "redacted_payment_details": {
                  "id": "QAAAKJ6",
                  "bank_account_details": null,
                  "billing_address": {
                    "administrative_area": null,
                    "country_code": "US",
                    "dependent_locality": null,
                    "line_1": null,
                    "line_2": null,
                    "locality": null,
                    "name": null,
                    "postal_code": "12312",
                    "sorting_code": null
                  },
                  "billing_email_address": "",
                  "card_details": {
                    "brand": "VISA",
                    "checks": {
                      "address_line1_check": "STATE_INVALID",
                      "address_postal_code_check": "PASS",
                      "cvc_check": "PASS"
                    },
                    "exp_month": 12,
                    "exp_year": 2050,
                    "last4": "4242"
                  },
                  "is_default": true,
                  "type": "CARD"
              }
            }
        """.trimIndent()
    )
}
