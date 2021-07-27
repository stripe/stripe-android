package com.stripe.android.model

import org.json.JSONObject

internal object PaymentMethodPreferenceFixtures {
    val EXPANDED_PAYMENT_INTENT_JSON = JSONObject(
        """
        {
          "object": "payment_method_preference",
          "country_code": "US",
          "ordered_payment_method_types": [
            "card",
            "sepa_debit",
            "sofort",
            "ideal",
            "bancontact"
          ],
          "payment_intent": {
            "id": "pi_1JGB5bIyGgrkZxL74Uk2VygL",
            "object": "payment_intent",
            "amount": 973,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "client_secret": "pi_1JGB5bIyGgrkZxL74Uk2VygL_secret_v5JtXY8PPJ9YicEKRyqSVKfHd",
            "confirmation_method": "automatic",
            "created": 1626995643,
            "currency": "eur",
            "description": null,
            "last_payment_error": null,
            "livemode": false,
            "next_action": null,
            "payment_method": null,
            "payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "requires_payment_method"
          },
          "type": "payment_intent"
        }
        """.trimIndent()
    )

    val EXPANDED_SETUP_INTENT_JSON = JSONObject(
        """
        {
          "object": "payment_method_preference",
          "country_code": "US",
          "ordered_payment_method_types": [
            "bancontact",
            "ideal",
            "sofort",
            "sepa_debit",
            "card"
          ],
          "setup_intent": {
            "id": "seti_1JGC8AIyGgrkZxL7QR3c4lWN",
            "object": "setup_intent",
            "cancellation_reason": null,
            "client_secret": "seti_1JGC8AIyGgrkZxL7QR3c4lWN_secret_Ju08uANFNpVJmZdKsfK5COMf921xCzg",
            "created": 1626999646,
            "description": null,
            "last_setup_error": null,
            "livemode": false,
            "next_action": null,
            "payment_method": null,
            "payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "status": "requires_payment_method",
            "usage": "off_session"
          },
          "type": "setup_intent"
        }
        """.trimIndent()
    )
}
