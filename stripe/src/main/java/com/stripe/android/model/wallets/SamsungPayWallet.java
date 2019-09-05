package com.stripe.android.model.wallets;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public final class SamsungPayWallet extends Wallet {
    private SamsungPayWallet(@NonNull Builder builder) {
        super(Type.SamsungPay, builder);
    }

    private SamsungPayWallet(@NonNull Parcel in) {
        super(in);
    }

    @NonNull
    static SamsungPayWallet.Builder fromJson() {
        return new Builder();
    }

    public static final class Builder extends Wallet.Builder<SamsungPayWallet> {
        @NonNull
        @Override
        SamsungPayWallet build() {
            return new SamsungPayWallet(this);
        }
    }

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
