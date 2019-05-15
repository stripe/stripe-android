package com.stripe.android.model;

public final class PaymentIntentFixtures {
    public static final PaymentIntent.RedirectData REDIRECT_DATA =
            new PaymentIntent.RedirectData("https://example.com",
                    "yourapp://post-authentication-return-url");
}
