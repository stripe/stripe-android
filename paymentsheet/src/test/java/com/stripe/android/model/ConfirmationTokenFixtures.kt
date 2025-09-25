package com.stripe.android.model

internal object ConfirmationTokenFixtures {

    val CONFIRMATION_TOKEN = ConfirmationToken(
        id = "ctoken_1234567890",
        created = 1694025025,
        expiresAt = 1694068225,
        liveMode = false,
        paymentIntentId = null,
        paymentMethodPreview = ConfirmationToken.PaymentMethodPreview(
            type = PaymentMethod.Type.Card,
            allResponseFields = """
                    {
                      "id": "pm_1234567890",
                      "object": "payment_method",
                      "type": "card",
                      "card": {
                        "brand": "visa",
                        "last4": "4242",
                        "exp_month": 8,
                        "exp_year": 2025
                      }
                    }
            """.trimIndent()
        ),
        returnUrl = null,
        setupFutureUsage = null,
        setupIntentId = null,
        shipping = null,
    )
}
