package com.stripe.android.model

import org.json.JSONObject

object PaymentMethodOptionsFixtures {
    internal val PI_CARD_PAYMENT_METHOD_OPTIONS_JSON = JSONObject(
        """
        {
            "installments": null,
            "mandate_options": null,
            "network": null,
            "request_three_d_secure": "automatic",
            "setup_future_usage": "on_session"
        }
        """.trimIndent()
    )

    internal val PI_US_BANK_ACCOUNT_PAYMENT_METHOD_OPTIONS_JSON = JSONObject(
        """
        {
            "setup_future_usage": "on_session",
            "verification_method": "microdeposits",
        }
        """.trimIndent()
    )

    internal val SI_CARD_PAYMENT_METHOD_OPTIONS_JSON = JSONObject(
        """
        {
            "mandate_options": null,
            "network": null,
            "request_three_d_secure": "automatic",
        }
        """.trimIndent()
    )

    internal val SI_US_BANK_ACCOUNT_PAYMENT_METHOD_OPTIONS_JSON = JSONObject(
        """
        {
            "verification_method": "automatic",
        }
        """.trimIndent()
    )
}
