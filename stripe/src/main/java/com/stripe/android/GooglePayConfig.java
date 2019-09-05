package com.stripe.android;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("WeakerAccess")
public final class GooglePayConfig {
    @NonNull private final String mPublishableKey;
    @NonNull private final String mApiVersion;

    /**
     * Instantiate with {@link PaymentConfiguration}. {@link PaymentConfiguration} must be
     * initialized.
     */
    public GooglePayConfig(@NonNull Context context) {
        this(PaymentConfiguration.getInstance(context).getPublishableKey());
    }

    public GooglePayConfig(@NonNull String publishableKey) {
        mPublishableKey = ApiKeyValidator.get().requireValid(publishableKey);
        mApiVersion = ApiVersion.get().getCode();
    }

    /**
     * @return a {@link JSONObject} representing a
     * <a href="https://developers.google.com/pay/api/android/reference/object#gateway">
     *     Google Pay TokenizationSpecification</a> configured for Stripe
     */
    @NonNull
    public JSONObject getTokenizationSpecification() throws JSONException {
        return new JSONObject()
                .put("type", "PAYMENT_GATEWAY")
                .put(
                        "parameters",
                        new JSONObject()
                                .put("gateway", "stripe")
                                .put("stripe:version", mApiVersion)
                                .put("stripe:publishableKey", mPublishableKey)
                );
    }
}
