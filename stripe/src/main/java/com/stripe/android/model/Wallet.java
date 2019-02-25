package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import static com.stripe.android.model.StripeJsonUtils.optString;

abstract class Wallet extends StripeJsonModel {
    private static final String FIELD_TYPE = "type";

    @Nullable
    static Wallet fromJson(@Nullable JSONObject walletJson) {
        if (walletJson == null) {
            return null;
        }

        final Type walletType = Type.fromCode(optString(walletJson, FIELD_TYPE));
        if (walletType == null) {
            return null;
        }

        return create(walletType, walletJson);
    }

    @Nullable
    private static Wallet create(@NonNull Type walletType, @NonNull JSONObject walletJson) {
        switch (walletType) {
            case AmexExpressCheckout: {
                // TODO(mshafrir): implemented
                return null;
            }
            case ApplePay: {
                // TODO(mshafrir): implemented
                return null;
            }
            case GooglePay: {
                // TODO(mshafrir): implemented
                return null;
            }
            case Masterpass: {
                // TODO(mshafrir): implemented
                return null;
            }
            case Microsoft: {
                // TODO(mshafrir): implemented
                return null;
            }
            case SamsungPay: {
                // TODO(mshafrir): implemented
                return null;
            }
            case VisaCheckout: {
                // TODO(mshafrir): implemented
                return null;
            }
            default: {
                return null;
            }
        }
    }

    enum Type {
        AmexExpressCheckout("amex_express_checkout"),
        ApplePay("apple_pay"),
        GooglePay("google_pay"),
        Masterpass("master_pass"),
        Microsoft("microsoft"),
        SamsungPay("samsung_pay"),
        VisaCheckout("visa_checkout");

        @NonNull private final String mCode;

        Type(@NonNull String code) {
            mCode = code;
        }

        @Nullable
        static Type fromCode(@Nullable String code) {
            for (Type type : values()) {
                if (type.mCode.equals(code)) {
                    return type;
                }
            }

            return null;
        }
    }
}
