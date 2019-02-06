package com.stripe.android;

public class StripeErrorFixtures {
    public final static StripeError INVALID_REQUEST_ERROR = new StripeError(
            "invalid_request_error",
            "This payment method (bancontact) is not activated for your account.",
            "payment_method_unactivated",
            "type",
            "",
            "");
}
