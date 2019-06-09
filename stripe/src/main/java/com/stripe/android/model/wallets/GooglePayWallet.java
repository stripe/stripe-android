package com.stripe.android.model.wallets;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class GooglePayWallet extends Wallet {
    private GooglePayWallet(@NonNull Builder builder) {
        super(Type.GooglePay, builder);
    }

    private GooglePayWallet(@NonNull Parcel in) {
        super(in);
    }

    @NonNull
    @Override
    Map<String, Object> getWalletTypeMap() {
        return new HashMap<>();
    }

    @NonNull
    static GooglePayWallet.Builder fromJson(@NonNull JSONObject walletJson) {
        return new Builder();
    }

    public static final class Builder extends Wallet.Builder<GooglePayWallet> {
        @NonNull
        @Override
        GooglePayWallet build() {
            return new GooglePayWallet(this);
        }
    }

    public static final Parcelable.Creator<GooglePayWallet> CREATOR =
            new Parcelable.Creator<GooglePayWallet>() {
                @Override
                public GooglePayWallet createFromParcel(@NonNull Parcel in) {
                    return new GooglePayWallet(in);
                }

                @Override
                public GooglePayWallet[] newArray(int size) {
                    return new GooglePayWallet[size];
                }
            };
}
