package com.stripe.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import java.util.Currency;
import java.util.Objects;

/**
 * Model representing a shipping method in the Android SDK.
 */
public final class ShippingMethod extends StripeModel implements Parcelable {

    public static final Parcelable.Creator<ShippingMethod> CREATOR
            = new Parcelable.Creator<ShippingMethod>() {
        public ShippingMethod createFromParcel(Parcel in) {
            return new ShippingMethod(in);
        }

        public ShippingMethod[] newArray(int size) {
            return new ShippingMethod[size];
        }
    };

    private final long mAmount;
    @NonNull @Size(min = 0, max = 3) private final String mCurrencyCode;
    @Nullable private final String mDetail;
    @NonNull private final String mIdentifier;
    @NonNull private final String mLabel;

    public ShippingMethod(@NonNull String label,
                          @NonNull String identifier,
                          long amount,
                          @NonNull @Size(min = 0, max = 3) String currencyCode) {
        this(label, identifier, null, amount, currencyCode);
    }

    public ShippingMethod(@NonNull String label,
                          @NonNull String identifier,
                          @Nullable String detail,
                          long amount,
                          @NonNull @Size(min = 0, max = 3) String currencyCode) {
        mLabel = label;
        mIdentifier = identifier;
        mDetail = detail;
        mAmount = amount;
        mCurrencyCode = currencyCode;
    }

    private ShippingMethod(@NonNull Parcel in) {
        mAmount = in.readLong();
        mCurrencyCode = Objects.requireNonNull(in.readString());
        mDetail = in.readString();
        mIdentifier = Objects.requireNonNull(in.readString());
        mLabel = Objects.requireNonNull(in.readString());
    }

    /**
     * @return the currency that the specified amount will be rendered in.
     */
    @NonNull
    public Currency getCurrency() {
        return Currency.getInstance(mCurrencyCode);
    }

    /**
     * @return The cost in minor unit of the currency provided in the
     * {@link com.stripe.android.PaymentConfiguration}. For example, cents in the USA and yen in
     * Japan.
     */
    public long getAmount() {
        return mAmount;
    }

    /**
     * @return Human friendly label specifying the shipping method that can be shown in the UI.
     */
    @NonNull
    public String getLabel() {
        return mLabel;
    }

    /**
     * @return Human friendly information such as estimated shipping times that can be shown in
     * the UI
     */
    @Nullable
    public String getDetail() {
        return mDetail;
    }

    /**
     * @return Identifier for the shipping method.
     */
    @NonNull
    public String getIdentifier() {
        return mIdentifier;
    }

    /************** Parcelable *********************/
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeLong(mAmount);
        parcel.writeString(mCurrencyCode);
        parcel.writeString(mDetail);
        parcel.writeString(mIdentifier);
        parcel.writeString(mLabel);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof ShippingMethod && typedEquals((ShippingMethod) obj));
    }

    private boolean typedEquals(@NonNull ShippingMethod shippingMethod) {
        return mAmount == shippingMethod.mAmount
                && Objects.equals(mCurrencyCode, shippingMethod.mCurrencyCode)
                && Objects.equals(mDetail, shippingMethod.mDetail)
                && Objects.equals(mIdentifier, shippingMethod.mIdentifier)
                && Objects.equals(mLabel, shippingMethod.mLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAmount, mCurrencyCode, mDetail, mIdentifier, mLabel);
    }
}
