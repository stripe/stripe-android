package com.stripe.android.model.wallets;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public final class AmexExpressCheckoutWallet extends Wallet {
    private AmexExpressCheckoutWallet(@NonNull Builder builder) {
        super(Type.AmexExpressCheckout, builder);
    }

    private AmexExpressCheckoutWallet(@NonNull Parcel in) {
        super(in);
    }

    @NonNull
    static AmexExpressCheckoutWallet.Builder fromJson() {
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
