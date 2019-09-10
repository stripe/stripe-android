package com.stripe.android.model.wallets;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import org.json.JSONObject;

import static com.stripe.android.model.StripeJsonUtils.optString;

public final class MasterpassWallet extends Wallet {
    private static final String FIELD_BILLING_ADDRESS = "billing_address";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_SHIPPING_ADDRESS = "shipping_address";

    @Nullable public final Address billingAddress;
    @Nullable public final String email;
    @Nullable public final String name;
    @Nullable public final Address shippingAddress;

    private MasterpassWallet(@NonNull Builder builder) {
        super(Type.Masterpass, builder);
        billingAddress = builder.mBillingAddress;
        email = builder.mEmail;
        name = builder.mName;
        shippingAddress = builder.mShippingAddress;
    }

    @NonNull
    static MasterpassWallet.Builder fromJson(@NonNull JSONObject wallet) {
        return new Builder()
                .setBillingAddress(Address.fromJson(wallet.optJSONObject(FIELD_BILLING_ADDRESS)))
                .setEmail(optString(wallet, FIELD_EMAIL))
                .setName(optString(wallet, FIELD_NAME))
                .setShippingAddress(Address.fromJson(wallet.optJSONObject(FIELD_SHIPPING_ADDRESS)));
    }

    @Override
    public int hashCode() {
        return Objects.hash(billingAddress, email, name, shippingAddress);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof MasterpassWallet
                && typedEquals((MasterpassWallet) obj));
    }

    private boolean typedEquals(@NonNull MasterpassWallet wallet) {
        return Objects.equals(billingAddress, wallet.billingAddress)
                && Objects.equals(email, wallet.email)
                && Objects.equals(name, wallet.name)
                && Objects.equals(shippingAddress, wallet.shippingAddress);
    }

    public static final class Builder extends Wallet.Builder<MasterpassWallet> {
        @Nullable private Address mBillingAddress;
        @Nullable private String mEmail;
        @Nullable private String mName;
        @Nullable private Address mShippingAddress;

        @NonNull
        public Builder setBillingAddress(@Nullable Address billingAddress) {
            this.mBillingAddress = billingAddress;
            return this;
        }

        @NonNull
        public Builder setEmail(@Nullable String email) {
            this.mEmail = email;
            return this;
        }

        @NonNull
        public Builder setName(@Nullable String name) {
            this.mName = name;
            return this;
        }

        @NonNull
        public Builder setShippingAddress(@Nullable Address shippingAddress) {
            this.mShippingAddress = shippingAddress;
            return this;
        }

        @NonNull
        public MasterpassWallet build() {
            return new MasterpassWallet(this);
        }
    }

    private MasterpassWallet(@NonNull Parcel in) {
        super(in);
        billingAddress = in.readParcelable(Address.class.getClassLoader());
        email = in.readString();
        name = in.readString();
        shippingAddress = in.readParcelable(Address.class.getClassLoader());
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(billingAddress, flags);
        dest.writeString(email);
        dest.writeString(name);
        dest.writeParcelable(shippingAddress, flags);
    }

    public static final Parcelable.Creator<MasterpassWallet> CREATOR =
            new Parcelable.Creator<MasterpassWallet>() {
                @Override
                public MasterpassWallet createFromParcel(@NonNull Parcel in) {
                    return new MasterpassWallet(in);
                }

                @Override
                public MasterpassWallet[] newArray(int size) {
                    return new MasterpassWallet[size];
                }
            };
}
