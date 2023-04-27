package com.stripe.android.model

import org.json.JSONObject

object PaymentMethodOptionsFixtures {
    internal val PI_PAYMENT_METHOD_OPTIONS_JSON = JSONObject(
        """
        {
            "card": {
                "installments": null,
                "mandate_options": null,
                "network": null,
                "request_three_d_secure": "automatic",
                "setup_future_usage": "on_session"
            },
            "us_bank_account": {
                "setup_future_usage": "on_session",
                "verification_method": "microdeposits",
            }
        }
        """.trimIndent()
    )

    internal val SI_PAYMENT_METHOD_OPTIONS_JSON = JSONObject(
        """
        {
            "card": {
                "mandate_options": null,
                "network": null,
                "request_three_d_secure": "automatic",
            },
            "us_bank_account": {
                "verification_method": "automatic",
            }
        }
        """.trimIndent()
    )
}
