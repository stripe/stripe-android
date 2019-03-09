package com.stripe.android.model.wallets;

import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class ApplePayWallet extends Wallet {
    private ApplePayWallet(@NonNull Builder builder) {
        super(Type.ApplePay, builder);
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
}
