package com.stripe.android.model.wallets;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.optString;

public final class VisaCheckoutWallet extends Wallet {
    private static final String FIELD_BILLING_ADDRESS = "billing_address";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_SHIPPING_ADDRESS = "shipping_address";

    @Nullable public final Address billingAddress;
    @Nullable public final String email;
    @Nullable public final String name;
    @Nullable public final Address shippingAddress;

    private VisaCheckoutWallet(@NonNull Builder builder) {
        super(Type.VisaCheckout, builder);
        billingAddress = builder.mBillingAddress;
        email = builder.mEmail;
        name = builder.mName;
        shippingAddress = builder.mShippingAddress;
    }

    @NonNull
    @Override
    Map<String, Object> getWalletTypeMap() {
        final AbstractMap<String, Object> wallet = new HashMap<>();
        wallet.put(FIELD_BILLING_ADDRESS,
                billingAddress != null ? billingAddress.toMap() : null);
        wallet.put(FIELD_EMAIL, email);
        wallet.put(FIELD_NAME, name);
        wallet.put(FIELD_SHIPPING_ADDRESS,
                shippingAddress != null ? shippingAddress.toMap() : null);
        return wallet;
    }

    @NonNull
    @Override
    JSONObject getWalletTypeJson() {
        final JSONObject wallet = new JSONObject();
        try {
            wallet.put(FIELD_BILLING_ADDRESS,
                    billingAddress != null ? billingAddress.toJson() : null);
            wallet.put(FIELD_EMAIL, email);
            wallet.put(FIELD_NAME, name);
            wallet.put(FIELD_SHIPPING_ADDRESS,
                    shippingAddress != null ? shippingAddress.toJson() : null);
        } catch (JSONException ignore) {}
        return wallet;
    }

    @NonNull
    static VisaCheckoutWallet.Builder fromJson(@NonNull JSONObject wallet) {
        return new Builder()
                .setBillingAddress(Address.fromJson(wallet.optJSONObject(FIELD_BILLING_ADDRESS)))
                .setEmail(optString(wallet, FIELD_EMAIL))
                .setName(optString(wallet, FIELD_NAME))
                .setShippingAddress(Address.fromJson(wallet.optJSONObject(FIELD_SHIPPING_ADDRESS)));
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(billingAddress, email, name, shippingAddress);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof VisaCheckoutWallet
                && typedEquals((VisaCheckoutWallet) obj));
    }

    private boolean typedEquals(@NonNull VisaCheckoutWallet wallet) {
        return ObjectUtils.equals(billingAddress, wallet.billingAddress)
                && ObjectUtils.equals(email, wallet.email)
                && ObjectUtils.equals(name, wallet.name)
                && ObjectUtils.equals(shippingAddress, wallet.shippingAddress);
    }

    public static final class Builder extends Wallet.Builder<VisaCheckoutWallet> {
        @Nullable private Address mBillingAddress;
        @Nullable private String mEmail;
        @Nullable private String mName;
        @Nullable private Address mShippingAddress;

        @NonNull
        public Builder setBillingAddress(@Nullable Address billingAddress) {
            this.mBillingAddress = billingAddress;
            return this;
        }

        @NonNull
        public Builder setEmail(@Nullable String email) {
            this.mEmail = email;
            return this;
        }

        @NonNull
        public Builder setName(@Nullable String name) {
            this.mName = name;
            return this;
        }

        @NonNull
        public Builder setShippingAddress(@Nullable Address shippingAddress) {
            this.mShippingAddress = shippingAddress;
            return this;
        }

        @NonNull
        public VisaCheckoutWallet build() {
            return new VisaCheckoutWallet(this);
        }
    }
}
