package com.stripe.android.model.wallets;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class SamsungPayWallet extends Wallet {
    private SamsungPayWallet(@NonNull Builder builder) {
        super(Type.SamsungPay, builder);
    }

    private SamsungPayWallet(@NonNull Parcel in) {
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
    static SamsungPayWallet.Builder fromJson(@NonNull JSONObject walletJson) {
        return new Builder();
    }

    public static final class Builder extends Wallet.Builder<SamsungPayWallet> {
        @NonNull
        @Override
        SamsungPayWallet build() {
            return new SamsungPayWallet(this);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<SamsungPayWallet> CREATOR =
            new Parcelable.Creator<SamsungPayWallet>() {
                @Override
                public SamsungPayWallet createFromParcel(@NonNull Parcel in) {
                    return new SamsungPayWallet(in);
                }

                @Override
                public SamsungPayWallet[] newArray(int size) {
                    return new SamsungPayWallet[size];
                }
            };
}
