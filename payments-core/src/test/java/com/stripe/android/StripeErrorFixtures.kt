package com.stripe.android

import com.stripe.android.core.StripeError

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
}
