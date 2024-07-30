package com.stripe.android.stripe3ds2.init.ui;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;
import com.stripe.android.stripe3ds2.utils.CustomizeUtils;
import com.stripe.android.stripe3ds2.utils.ObjectUtils;

public final class StripeTextBoxCustomization extends BaseCustomization
        implements TextBoxCustomization, Parcelable {
    public static final Creator<StripeTextBoxCustomization> CREATOR =
            new Creator<StripeTextBoxCustomization>() {
                @Override
                public StripeTextBoxCustomization createFromParcel(@NonNull Parcel in) {
                    return new StripeTextBoxCustomization(in);
                }

                @Override
                public StripeTextBoxCustomization[] newArray(int size) {
                    return new StripeTextBoxCustomization[size];
                }
            };

    private int mBorderWidth;
    @Nullable private String mBorderColor;
    private int mCornerRadius;
    @Nullable private String mHintTextColor;

    public StripeTextBoxCustomization() {
        super();
    }

    private StripeTextBoxCustomization(@NonNull Parcel in) {
        super(in);
        mBorderWidth = in.readInt();
        mBorderColor = in.readString();
        mCornerRadius = in.readInt();
        mHintTextColor = in.readString();
    }


    @Override
    public void setBorderWidth(int borderWidth) throws InvalidInputException {
        mBorderWidth = CustomizeUtils.requireValidDimension(borderWidth);
    }

    @Override
    public int getBorderWidth() {
        return mBorderWidth;
    }

    @Override
    public void setBorderColor(@NonNull String borderColor) throws InvalidInputException {
        mBorderColor = CustomizeUtils.requireValidColor(borderColor);
    }

    @Nullable
    @Override
    public String getBorderColor() {
        return mBorderColor;
    }

    @Override
    public void setCornerRadius(int cornerRadius) throws InvalidInputException {
        mCornerRadius = CustomizeUtils.requireValidDimension(cornerRadius);
    }

    @Override
    public int getCornerRadius() {
        return mCornerRadius;
    }

    @Override
    public void setHintTextColor(@NonNull String hintTextColor) throws InvalidInputException {
        mHintTextColor = CustomizeUtils.requireValidColor(hintTextColor);
    }

    @Nullable
    @Override
    public String getHintTextColor() {
        return mHintTextColor;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof StripeTextBoxCustomization
                && typedEquals((StripeTextBoxCustomization) obj));
    }

    private boolean typedEquals(@NonNull StripeTextBoxCustomization textBoxCustomization) {
        return mBorderWidth == textBoxCustomization.mBorderWidth
                && ObjectUtils.equals(mBorderColor, textBoxCustomization.mBorderColor)
                && mCornerRadius == textBoxCustomization.mCornerRadius
                && ObjectUtils.equals(mHintTextColor, textBoxCustomization.mHintTextColor);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mBorderWidth, mBorderColor, mCornerRadius, mHintTextColor);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mBorderWidth);
        dest.writeString(mBorderColor);
        dest.writeInt(mCornerRadius);
        dest.writeString(mHintTextColor);
    }
}
