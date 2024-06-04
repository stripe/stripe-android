package com.stripe.android.model

import com.stripe.android.model.parsers.ConsumerPaymentDetailsJsonParser
import org.json.JSONObject

internal object PaymentDetailsFixtures {
    val CONSUMER_SINGLE_PAYMENT_DETAILS_JSON = JSONObject(
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
    val CONSUMER_SINGLE_PAYMENT_DETAILS =
        ConsumerPaymentDetailsJsonParser().parse(CONSUMER_SINGLE_PAYMENT_DETAILS_JSON)
}
