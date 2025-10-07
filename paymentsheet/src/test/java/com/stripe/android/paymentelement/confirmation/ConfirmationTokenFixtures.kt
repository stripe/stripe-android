package com.stripe.android.paymentelement.confirmation

import org.json.JSONObject

internal object ConfirmationTokenFixtures {

    val CONFIRMATION_TOKEN_JSON = JSONObject(
        """
        {
          "id": "ctoken_1NnQUf2eZvKYlo2CIObdtbnb",
          "object": "confirmation_token",
          "created": 1694025025,
          "expires_at": 1694068225,
          "livemode": false,
          "mandate_data": null,
          "payment_intent": null,
          "payment_method": null,
          "payment_method_preview": {
            "billing_details": {
              "address": {
                "city": "Hyde Park",
                "country": "US",
                "line1": "50 Sprague St",
                "line2": "",
                "postal_code": "02136",
                "state": "MA"
              },
              "email": "jennyrosen@stripe.com",
              "name": "Jenny Rosen",
              "phone": null
            },
            "card": {
              "brand": "visa",
              "checks": {
                "address_line1_check": null,
                "address_postal_code_check": null,
                "cvc_check": null
              },
              "country": "US",
              "display_brand": "visa",
              "exp_month": 8,
              "exp_year": 2026,
              "funding": "credit",
              "generated_from": null,
              "last4": "4242",
              "networks": {
                "available": [
                  "visa"
                ],
                "preferred": null
              },
              "three_d_secure_usage": {
                "supported": true
              },
              "wallet": null
            },
            "type": "card"
          },
          "return_url": "https://example.com/return",
          "setup_future_usage": "off_session",
          "setup_intent": null,
          "shipping": {
            "address": {
              "city": "Hyde Park",
              "country": "US",
              "line1": "50 Sprague St",
              "line2": "",
              "postal_code": "02136",
              "state": "MA"
            },
            "name": "Jenny Rosen",
            "phone": null
          }
        }
        """.trimIndent()
    )

    val MINIMAL_CONFIRMATION_TOKEN_JSON = JSONObject(
        """
        {
          "id": "ctoken_1234567890",
          "object": "confirmation_token",
          "created": 1694025025,
          "livemode": false
        }
        """.trimIndent()
    )
}
