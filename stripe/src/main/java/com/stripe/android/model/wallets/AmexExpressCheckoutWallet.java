package com.stripe.android.model.wallets;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class AmexExpressCheckoutWallet extends Wallet {
    private AmexExpressCheckoutWallet(@NonNull Builder builder) {
        super(Type.AmexExpressCheckout, builder);
    }

    private AmexExpressCheckoutWallet(@NonNull Parcel in) {
        super(in);
    }

    @NonNull
    @Override
    Map<String, Object> getWalletTypeMap() {
        return new HashMap<>();
    }

    @NonNull
    static AmexExpressCheckoutWallet.Builder fromJson(@NonNull JSONObject walletJson) {
        return new Builder();
    }

    public static final class Builder extends Wallet.Builder<AmexExpressCheckoutWallet> {
        @NonNull
        @Override
        AmexExpressCheckoutWallet build() {
            return new AmexExpressCheckoutWallet(this);
        }
    }

    public static final Parcelable.Creator<AmexExpressCheckoutWallet> CREATOR =
            new Parcelable.Creator<AmexExpressCheckoutWallet>() {
                @Override
                public AmexExpressCheckoutWallet createFromParcel(@NonNull Parcel in) {
                    return new AmexExpressCheckoutWallet(in);
                }

                @Override
                public AmexExpressCheckoutWallet[] newArray(int size) {
                    return new AmexExpressCheckoutWallet[size];
                }
            };
}
