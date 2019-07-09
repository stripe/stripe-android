package com.stripe.android.model;

import android.support.annotation.NonNull;

import java.util.Objects;

public final class Stripe3dsRedirect {
    private static final String FIELD_STRIPE_JS = "stripe_js";

    @NonNull private final String mUrl;

    private Stripe3dsRedirect(@NonNull String url) {
        mUrl = url;
    }

    @NonNull
    public static Stripe3dsRedirect create(@NonNull StripeIntent.SdkData sdkData) {
        if (!sdkData.is3ds1()) {
            throw new IllegalArgumentException(
                    "Expected SdkData with type='three_d_secure_redirect'.");
        }

        return new Stripe3dsRedirect(
                (String) Objects.requireNonNull(sdkData.data.get(FIELD_STRIPE_JS))
        );
    }

    @NonNull
    public StripeIntent.RedirectData getRedirectData() {
        return new StripeIntent.RedirectData(mUrl, null);
    }
}
