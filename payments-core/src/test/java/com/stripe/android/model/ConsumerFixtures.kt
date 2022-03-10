package com.stripe.android.model

import com.stripe.android.model.parsers.ConsumerPaymentDetailsJsonParser
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

    val CONSUMER_PAYMENT_DETAILS_JSON = JSONObject(
        """
            {
              "redacted_payment_details": [
                {
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
                    "brand": "MASTERCARD",
                    "checks": {
                      "address_line1_check": "STATE_INVALID",
                      "address_postal_code_check": "PASS",
                      "cvc_check": "PASS"
                    },
                    "exp_month": 12,
                    "exp_year": 2023,
                    "last4": "4444"
                  },
                  "is_default": true,
                  "type": "CARD"
                },
                {
                  "id": "QAAAKIL",
                  "bank_account_details": null,
                  "billing_address": {
                    "administrative_area": null,
                    "country_code": "US",
                    "dependent_locality": null,
                    "line_1": null,
                    "line_2": null,
                    "locality": null,
                    "name": null,
                    "postal_code": "42424",
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
                    "exp_month": 4,
                    "exp_year": 2024,
                    "last4": "4242"
                  },
                  "is_default": false,
                  "type": "CARD"
                }
              ]
            }
        """.trimIndent()
    )
    val CONSUMER_PAYMENT_DETAILS =
        ConsumerPaymentDetailsJsonParser().parse(CONSUMER_PAYMENT_DETAILS_JSON)
}
