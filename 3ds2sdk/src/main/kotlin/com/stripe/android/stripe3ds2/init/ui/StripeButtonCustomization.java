package com.stripe.android.stripe3ds2.init.ui;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;
import com.stripe.android.stripe3ds2.utils.CustomizeUtils;
import com.stripe.android.stripe3ds2.utils.ObjectUtils;

public final class StripeButtonCustomization extends BaseCustomization
        implements ButtonCustomization, Parcelable {

    public static final Creator<StripeButtonCustomization> CREATOR =
            new Creator<StripeButtonCustomization>() {
                @Override
                public StripeButtonCustomization createFromParcel(Parcel in) {
                    return new StripeButtonCustomization(in);
                }

                @Override
                public StripeButtonCustomization[] newArray(int size) {
                    return new StripeButtonCustomization[size];
                }
            };

    @Nullable private String mBackgroundColor;
    private int mCornerRadius;

    public StripeButtonCustomization() {
        super();
    }

    private StripeButtonCustomization(@NonNull Parcel in) {
        super(in);
        mBackgroundColor = in.readString();
        mCornerRadius = in.readInt();
    }

    @Override
    public void setBackgroundColor(@NonNull String hexColorCode) throws InvalidInputException {
        mBackgroundColor = CustomizeUtils.requireValidColor(hexColorCode);
    }

    @Override
    public void setCornerRadius(int cornerRadius) throws InvalidInputException {
        mCornerRadius = CustomizeUtils.requireValidDimension(cornerRadius);
    }

    @Nullable
    @Override
    public String getBackgroundColor() {
        return mBackgroundColor;
    }

    @Override
    public int getCornerRadius() {
        return mCornerRadius;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof StripeButtonCustomization
                && typedEquals((StripeButtonCustomization) obj));
    }

    private boolean typedEquals(@NonNull StripeButtonCustomization buttonCustomization) {
        return ObjectUtils.equals(mBackgroundColor, buttonCustomization.mBackgroundColor)
                && mCornerRadius == buttonCustomization.mCornerRadius;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mBackgroundColor, mCornerRadius);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mBackgroundColor);
        dest.writeInt(mCornerRadius);
    }
}
