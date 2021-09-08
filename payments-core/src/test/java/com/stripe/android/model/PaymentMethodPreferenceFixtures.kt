package com.stripe.android.model

import org.json.JSONObject

internal object PaymentMethodPreferenceFixtures {
    val EXPANDED_PAYMENT_INTENT_JSON = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "link_settings": {
            "link_bank_enabled": false,
            "link_bank_onboarding_enabled": false
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "payment_intent": {
              "id": "pi_3JTDhYIyGgrkZxL71IDUGKps",
              "object": "payment_intent",
              "amount": 973,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "client_secret": "pi_3JTDhYIyGgrkZxL71IDUGKps_secret_aWuzwD4JvF1HM8XJTdUsXG6Za",
              "confirmation_method": "automatic",
              "created": 1630103948,
              "currency": "eur",
              "description": null,
              "last_payment_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "bancontact",
                "card",
                "sepa_debit",
                "sofort",
                "ideal"
              ],
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": {
                "address": {
                  "city": "San Francisco",
                  "country": "US",
                  "line1": "510 Townsend St",
                  "line2": null,
                  "postal_code": "94102",
                  "state": "California"
                },
                "carrier": null,
                "name": "Bruno",
                "phone": null,
                "tracking_number": null
              },
              "source": null,
              "status": "requires_payment_method"
            },
            "type": "payment_intent"
          }
        }
        """.trimIndent()
    )

    val EXPANDED_SETUP_INTENT_JSON = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "link_settings": {
            "link_bank_enabled": false,
            "link_bank_onboarding_enabled": false
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "setup_intent": {
              "id": "seti_1JTDqGIyGgrkZxL7reCXkpr5",
              "object": "setup_intent",
              "cancellation_reason": null,
              "client_secret": "seti_1JTDqGIyGgrkZxL7reCXkpr5_secret_K7SlVPncyaU4cdiDyyHfyqNjvCDvYxG",
              "created": 1630104488,
              "description": null,
              "last_setup_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "sepa_debit",
                "ideal",
                "bancontact",
                "card",
                "sofort"
              ],
              "status": "requires_payment_method",
              "usage": "off_session"
            },
            "type": "setup_intent"
          }
        }
        """.trimIndent()
    )
}
