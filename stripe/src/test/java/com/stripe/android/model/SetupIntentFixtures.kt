package com.stripe.android.model

internal object SetupIntentFixtures {

    @JvmField
    internal val SI_NEXT_ACTION_REDIRECT_JSON =
        """
        {
            "id": "seti_1EqTSZGMT9dGPIDGVzCUs6dV",
            "object": "setup_intent",
            "cancellation_reason": null,
            "client_secret": "seti_1EqTSZGMT9dGPIDGVzCUs6dV_secret_FL9mS9ILygVyGEOSmVNqHT83rxkqy0Y",
            "created": 1561677666,
            "description": "a description",
            "last_setup_error": null,
            "livemode": false,
            "next_action": {
                "redirect_to_url": {
                    "return_url": "stripe://setup_intent_return",
                    "url": "https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02"
                },
                "type": "redirect_to_url"
            },
            "payment_method": "pm_1EqTSoGMT9dGPIDG7dgafX1H",
            "payment_method_types": [
                "card"
            ],
            "status": "requires_action",
            "usage": "off_session"
        }
        """.trimIndent()

    @JvmField
    internal val SI_WITH_LAST_PAYMENT_ERROR = SetupIntent.fromString(
        """
        {
            "id": "seti_1EqTSZGMT9dGPIDGVzCUs6dV",
            "object": "setup_intent",
            "cancellation_reason": null,
            "client_secret": "seti_1EqTSZGMT9dGPIDGVzCUs6dV_secret_FL9mS9ILygVyGEOSmVNqHT83rxkqy0Y",
            "created": 1561677666,
            "description": "a description",
            "last_setup_error": {
                "code": "payment_intent_authentication_failure",
                "doc_url": "https://stripe.com/docs/error-codes/payment-intent-authentication-failure",
                "message": "The provided PaymentMethod has failed authentication. You can provide payment_method_data or a new PaymentMethod to attempt to fulfill this PaymentIntent again.",
                "payment_method": {
                    "id": "pm_1F7J1bCRMbs6FrXfQKsYwO3U",
                    "object": "payment_method",
                    "billing_details": {
                        "address": {
                            "city": null,
                            "country": null,
                            "line1": null,
                            "line2": null,
                            "postal_code": null,
                            "state": null
                        },
                        "email": null,
                        "name": null,
                        "phone": null
                    },
                    "card": {
                        "brand": "visa",
                        "checks": {
                            "address_line1_check": null,
                            "address_postal_code_check": null,
                            "cvc_check": null
                        },
                        "country": null,
                        "exp_month": 8,
                        "exp_year": 2020,
                        "funding": "credit",
                        "generated_from": null,
                        "last4": "3220",
                        "three_d_secure_usage": {
                            "supported": true
                        },
                        "wallet": null
                    },
                    "created": 1565775851,
                    "customer": null,
                    "livemode": false,
                    "metadata": {},
                    "type": "card"
                },
                "type": "invalid_request_error"
            },
            "livemode": false,
            "next_action": {
                "redirect_to_url": {
                    "return_url": "stripe://setup_intent_return",
                    "url": "https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02"
                },
                "type": "redirect_to_url"
            },
            "payment_method": "pm_1EqTSoGMT9dGPIDG7dgafX1H",
            "payment_method_types": [
                "card"
            ],
            "status": "requires_action",
            "usage": "off_session"
        }
        """.trimIndent()
        )!!

    @JvmField
    internal val CANCELLED = SetupIntent.fromString(
        """
        {
            "id": "seti_1FCoS9CRMbs6FrXfxFQOp8Mm",
            "object": "setup_intent",
            "application": null,
            "cancellation_reason": "abandoned",
            "client_secret": "seti_1FCoS9CRMbs6FrXfxFQOp8Mm_secret_FiEwNDtwMi",
            "created": 1567088301,
            "customer": "cus_FWhpaTLIPWLhpJ",
            "description": null,
            "last_setup_error": null,
            "livemode": false,
            "metadata": {},
            "next_action": null,
            "on_behalf_of": null,
            "payment_method": "pm_1F1wa2CRMbs6FrXfm9XfWrGS",
            "payment_method_options": {
                "card": {
                    "request_three_d_secure": "automatic"
                }
            },
            "payment_method_types": [
                "card"
            ],
            "status": "canceled",
            "usage": "off_session"
        }
        """.trimIndent()
    )!!

    @JvmField
    val SI_NEXT_ACTION_REDIRECT = SetupIntent.fromString(SI_NEXT_ACTION_REDIRECT_JSON)!!
}
