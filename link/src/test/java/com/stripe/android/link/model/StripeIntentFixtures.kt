package com.stripe.android.link.model

import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.model.parsers.SetupIntentJsonParser
import org.json.JSONObject

internal object StripeIntentFixtures {
    private val PI_PARSER = PaymentIntentJsonParser()
    private val SI_PARSER = SetupIntentJsonParser()

    private val PI_SUCCEEDED_JSON = JSONObject(
        """
        {
            "id": "pi_1IRg6VCRMbs6F",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "client_secret": "pi_1IRg6VCRMbs6F_secret_7oH5g4v8GaCrHfsGYS6kiSnwF",
            "confirmation_method": "automatic",
            "created": 1614960135,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "last_payment_error": null,
            "livemode": false,
            "next_action": null,
            "payment_method": "pm_1IJs3ZCRMbs",
            "payment_method_types": ["card"],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "succeeded"
        }
        """.trimIndent()
    )
    val PI_SUCCEEDED = requireNotNull(PI_PARSER.parse(PI_SUCCEEDED_JSON))

    internal val SI_NEXT_ACTION_REDIRECT_JSON = JSONObject(
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
    )
    val SI_NEXT_ACTION_REDIRECT = requireNotNull(SI_PARSER.parse(SI_NEXT_ACTION_REDIRECT_JSON))
}
