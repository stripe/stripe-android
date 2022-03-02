package com.stripe.android.core

import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import org.json.JSONObject

internal object StripeErrorFixtures {
    @JvmField
    val INVALID_REQUEST_ERROR = StripeError(
        "invalid_request_error",
        "This payment method (bancontact) is not activated for your account.",
        "payment_method_unactivated",
        "type",
        "",
        ""
    )

    val INVALID_CARD_NUMBER = StripeErrorJsonParser().parse(
        JSONObject(
            """
        {
            "error": {
                "code": "incorrect_number",
                "doc_url": "https:\/\/stripe.com\/docs\/error-codes\/incorrect-number",
                "message": "Your card number is incorrect.",
                "param": "number",
                "type": "card_error"
            }
        }
            """.trimIndent()
        )
    )
}
