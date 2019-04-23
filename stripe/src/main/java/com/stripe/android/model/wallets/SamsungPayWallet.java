package com.stripe.android.model.wallets;

import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class SamsungPayWallet extends Wallet {
    private SamsungPayWallet(@NonNull Builder builder) {
        super(Type.SamsungPay, builder);
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
}
