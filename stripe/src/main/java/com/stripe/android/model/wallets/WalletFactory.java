package com.stripe.android.model.wallets;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import static com.stripe.android.model.StripeJsonUtils.optString;
import static com.stripe.android.model.wallets.Wallet.FIELD_DYNAMIC_LAST4;

public final class WalletFactory {

    @Nullable
    public Wallet create(@Nullable JSONObject walletJson) {
        if (walletJson == null) {
            return null;
        }

        final Wallet.Type walletType = Wallet.Type
                .fromCode(optString(walletJson, Wallet.FIELD_TYPE));
        if (walletType == null) {
            return null;
        }

        return create(walletType, walletJson);
    }

    @Nullable
    private Wallet create(@NonNull Wallet.Type walletType, @NonNull JSONObject walletJson) {
        final JSONObject walletTypeJson = walletJson.optJSONObject(walletType.code);
        if (walletTypeJson == null) {
            return null;
        }

        final Wallet.Builder walletBuilder;

        switch (walletType) {
            case AmexExpressCheckout: {
                walletBuilder = AmexExpressCheckoutWallet.fromJson();
                break;
            }
            case ApplePay: {
                walletBuilder = ApplePayWallet.fromJson();
                break;
            }
            case GooglePay: {
                walletBuilder = GooglePayWallet.fromJson();
                break;
            }
            case Masterpass: {
                walletBuilder = MasterpassWallet.fromJson(walletTypeJson);
                break;
            }
            case SamsungPay: {
                walletBuilder = SamsungPayWallet.fromJson();
                break;
            }
            case VisaCheckout: {
                walletBuilder = VisaCheckoutWallet.fromJson(walletTypeJson);
                break;
            }
            default: {
                return null;
            }
        }

        final String dynamicLast4 = optString(walletJson, FIELD_DYNAMIC_LAST4);
        return walletBuilder
                .setDynamicLast4(dynamicLast4)
                .build();
    }
}
