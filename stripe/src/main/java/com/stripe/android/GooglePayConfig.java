package com.stripe.android;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("WeakerAccess")
public final class GooglePayConfig {
    @NonNull private final String mPublishableKey;
    @NonNull private final String mApiVersion;
    @Nullable private final String mConnectedAccountId;

    /**
     * Instantiate with {@link PaymentConfiguration}. {@link PaymentConfiguration} must be
     * initialized.
     */
    public GooglePayConfig(@NonNull Context context) {
        this(PaymentConfiguration.getInstance(context).getPublishableKey(), null);
    }

    /**
     * Instantiate with {@link PaymentConfiguration} and Connect Account Id.
     * {@link PaymentConfiguration} must be initialized.
     */
    public GooglePayConfig(@NonNull Context context, @Nullable String connectedAccountId) {
        this(PaymentConfiguration.getInstance(context).getPublishableKey(), connectedAccountId);
    }

    public GooglePayConfig(@NonNull String publishableKey) {
        this(publishableKey, null);
    }

    public GooglePayConfig(@NonNull String publishableKey, @Nullable String connectedAccountId) {
        mPublishableKey = ApiKeyValidator.get().requireValid(publishableKey);
        mApiVersion = ApiVersion.get().getCode();
        mConnectedAccountId = connectedAccountId;
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
                                .put("stripe:publishableKey", getKey())
                );
    }

    @NonNull
    private String getKey() {
        if (mConnectedAccountId != null) {
            return String.format(Locale.ROOT, "%s/%s", mPublishableKey, mConnectedAccountId);
        } else {
            return mPublishableKey;
        }
    }
}
