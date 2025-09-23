package com.stripe.android.testing

import com.stripe.android.model.ConfirmationToken
import com.stripe.android.model.PaymentMethod

object ConfirmationTokenFactory {

    fun create(): ConfirmationToken {
        return ConfirmationToken(
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
}
