package com.stripe.android.model

import org.json.JSONObject

object ConsumerFixtures {

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

    val CONSUMER_LOGGED_OUT_JSON = JSONObject(
        """
            {
              "auth_session_client_secret": null,
              "consumer_session": {
                "client_secret": "TA3ZTctNDJhYi1iODI3LWY0NTVlZTdkM2Q2MzIIQWJCWlFibkk",
                "email_address": "test@stripe.com",
                "redacted_phone_number": "+1********23",
                "support_payment_details_types": [
                  "CARD"
                ],
                "verification_sessions": [
                  {
                    "state": "CANCELED",
                    "type": "SMS"
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
                    "brand": "MASTERCARD",
                    "checks": {
                      "address_line1_check": "STATE_INVALID",
                      "address_postal_code_check": "PASS",
                      "cvc_check": "PASS"
                    },
                    "funding": "CREDIT",
                    "exp_month": 12,
                    "exp_year": 2023,
                    "last4": "4444"
                  },
                  "is_default": true,
                  "type": "CARD"
              }
            }
        """.trimIndent()
    )

    val CONSUMER_SINGLE_CARD_PAYMENT_DETAILS_NULL_VALUES_JSON = JSONObject(
        """
            {
              "redacted_payment_details": {
                  "id": "QAAAKJ6",
                  "bank_account_details": null,
                  "billing_address": null,
                  "billing_email_address": "",
                  "card_details": {
                    "brand": "MASTERCARD",
                    "checks": null,
                    "funding": "CREDIT",
                    "exp_month": 12,
                    "exp_year": 2023,
                    "last4": "4444"
                  },
                  "is_default": null,
                  "type": "CARD"
              }
            }
        """.trimIndent()
    )

    val CONSUMER_SINGLE_BANK_ACCOUNT_PAYMENT_DETAILS_JSON = JSONObject(
        """
            {
              "redacted_payment_details": [
                {
                  "id": "wAAACGA",
                  "bank_account_details": {
                    "bank_icon_code": null,
                    "bank_name": "STRIPE TEST BANK",
                    "last4": "6789"
                  },
                  "billing_address": {
                    "administrative_area": null,
                    "country_code": "US",
                    "dependent_locality": null,
                    "line_1": "123 Fake St",
                    "line_2": null,
                    "locality": null,
                    "name": null,
                    "postal_code": "94103",
                    "sorting_code": null
                  },
                  "billing_email_address": "",
                  "card_details": null,
                  "is_default": true,
                  "type": "BANK_ACCOUNT"
                }
              ]
            }
        """.trimIndent()
    )

    val CONSUMER_SINGLE_BANK_ACCOUNT_PAYMENT_DETAILS_NULL_VALUES_JSON = JSONObject(
        """
            {
              "redacted_payment_details": [
                {
                  "id": "wAAACGA",
                  "bank_account_details": {
                    "bank_icon_code": null,
                    "bank_name": null,
                    "last4": "6789"
                  },
                  "billing_address": {
                    "administrative_area": null,
                    "country_code": null,
                    "dependent_locality": null,
                    "line_1": null,
                    "line_2": null,
                    "locality": null,
                    "name": null,
                    "postal_code": null,
                    "sorting_code": null
                  },
                  "billing_email_address": "",
                  "card_details": null,
                  "is_default": null,
                  "type": "BANK_ACCOUNT"
                }
              ]
            }
        """.trimIndent()
    )

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
                    "funding": "CREDIT",
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
                      "cvc_check": "FAIL"
                    },
                    "funding": "CREDIT",
                    "exp_month": 4,
                    "exp_year": 2024,
                    "last4": "4242"
                  },
                  "is_default": false,
                  "type": "CARD"
                },
                {
                  "id": "wAAACGA",
                  "bank_account_details": {
                    "bank_icon_code": null,
                    "bank_name": "STRIPE TEST BANK",
                    "last4": "6789"
                  },
                  "billing_address": {
                    "administrative_area": null,
                    "country_code": null,
                    "dependent_locality": null,
                    "line_1": null,
                    "line_2": null,
                    "locality": null,
                    "name": null,
                    "postal_code": null,
                    "sorting_code": null
                  },
                  "billing_email_address": "",
                  "card_details": null,
                  "is_default": false,
                  "type": "BANK_ACCOUNT"
                }
              ]
            }
        """.trimIndent()
    )

    val PAYMENT_DETAILS_SHARE_JSON = JSONObject(
        """
        {
            "object": "payment_method",
            "payment_method": {
                "id": "pm_1NsnWALu5o3P18Zp36Q7YfWW",
                "object": "payment_method",
                "created": 1550757934255,
                "customer": "cus_AQsHpvKfKwJDrF",
                "livemode": true,
                "type": "card",
                "billing_details": {
                    "address": {
                        "city": "San Francisco",
                        "country": "US",
                        "line1": "1234 Main Street",
                        "postal_code": "94111",
                        "state": "CA"
                    },
                    "email": "jenny.rosen@example.com",
                    "name": "Jenny Rosen",
                    "phone": "123-456-7890"
                },
                "card": {
                    "brand": "visa",
                    "checks": {
                        "address_line1_check": "unchecked",
                        "cvc_check": "unchecked"
                    },
                    "country": "US",
                    "exp_month": 8,
                    "exp_year": 2022,
                    "funding": "credit",
                    "fingerprint": "fingerprint123",
                    "last4": "4242",
                    "three_d_secure_usage": {
                        "supported": true
                    }
                }
            }
        }
        """.trimIndent()
    )
}
