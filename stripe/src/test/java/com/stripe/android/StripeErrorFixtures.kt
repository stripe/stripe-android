package com.stripe.android

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
