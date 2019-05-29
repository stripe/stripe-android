package com.stripe.android.model.wallets;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class ApplePayWallet extends Wallet {
    private ApplePayWallet(@NonNull Builder builder) {
        super(Type.ApplePay, builder);
    }

    private ApplePayWallet(@NonNull Parcel in) {
        super(in);
    }

    @NonNull
    @Override
    Map<String, Object> getWalletTypeMap() {
        return new HashMap<>();
    }

    @NonNull
    @Override
    JSONObject getWalletTypeJson() {
        return new JSONObject();
    }

    @NonNull
    static ApplePayWallet.Builder fromJson(@NonNull JSONObject walletJson) {
        return new Builder();
    }

    public static final class Builder extends Wallet.Builder<ApplePayWallet> {
        @NonNull
        @Override
        ApplePayWallet build() {
            return new ApplePayWallet(this);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ApplePayWallet> CREATOR =
            new Parcelable.Creator<ApplePayWallet>() {
                @Override
                public ApplePayWallet createFromParcel(@NonNull Parcel in) {
                    return new ApplePayWallet(in);
                }

                @Override
                public ApplePayWallet[] newArray(int size) {
                    return new ApplePayWallet[size];
                }
            };
}
