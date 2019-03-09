package com.stripe.android.model.wallets;

import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class AmexExpressCheckoutWallet extends Wallet {
    private AmexExpressCheckoutWallet(@NonNull Builder builder) {
        super(Type.AmexExpressCheckout, builder);
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
}
