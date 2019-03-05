package com.stripe.android.model.wallets;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class GooglePayWallet extends Wallet {
    private GooglePayWallet(@NonNull Builder builder) {
        super(Type.GooglePay, builder);
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
}
