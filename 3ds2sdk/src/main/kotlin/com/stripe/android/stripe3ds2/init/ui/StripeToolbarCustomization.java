package com.stripe.android.stripe3ds2.init.ui;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;
import com.stripe.android.stripe3ds2.utils.CustomizeUtils;
import com.stripe.android.stripe3ds2.utils.ObjectUtils;

public final class StripeToolbarCustomization extends BaseCustomization
        implements ToolbarCustomization, Parcelable {
    public static final Creator<StripeToolbarCustomization> CREATOR =
            new Creator<StripeToolbarCustomization>() {
                @Override
                public StripeToolbarCustomization createFromParcel(Parcel in) {
                    return new StripeToolbarCustomization(in);
                }

                @Override
                public StripeToolbarCustomization[] newArray(int size) {
                    return new StripeToolbarCustomization[size];
                }
            };

    @Nullable private String mBackgroundColor;
    @Nullable private String mStatusBarColor;
    @Nullable private String mHeaderText;
    @Nullable private String mButtonText;

    public StripeToolbarCustomization() {
        super();
    }

    private StripeToolbarCustomization(@NonNull Parcel in) {
        super(in);
        mBackgroundColor = in.readString();
        mStatusBarColor = in.readString();
        mHeaderText = in.readString();
        mButtonText = in.readString();
    }

    @Override
    public void setBackgroundColor(@NonNull String hexColorCode) throws InvalidInputException {
        mBackgroundColor = CustomizeUtils.requireValidColor(hexColorCode);
    }

    @Override
    public void setStatusBarColor(@NonNull String statusBarColor) throws InvalidInputException {
        mStatusBarColor = CustomizeUtils.requireValidColor(statusBarColor);
    }

    @Override
    public void setHeaderText(@NonNull String headerText) throws InvalidInputException {
        mHeaderText = CustomizeUtils.requireValidString(headerText);
    }

    @Override
    public void setButtonText(@NonNull String buttonText) throws InvalidInputException {
        mButtonText = CustomizeUtils.requireValidString(buttonText);
    }

    @Nullable
    @Override
    public String getBackgroundColor() {
        return mBackgroundColor;
    }

    @Nullable
    @Override
    public String getHeaderText() {
        return mHeaderText;
    }

    @Nullable
    @Override
    public String getButtonText() {
        return mButtonText;
    }

    @Override
    @Nullable
    public String getStatusBarColor() {
        return mStatusBarColor;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof StripeToolbarCustomization
                && typedEquals((StripeToolbarCustomization) obj));
    }

    private boolean typedEquals(@NonNull StripeToolbarCustomization toolbarCustomization) {
        return ObjectUtils.equals(mBackgroundColor, toolbarCustomization.mBackgroundColor)
                && ObjectUtils.equals(mStatusBarColor, toolbarCustomization.mStatusBarColor)
                && ObjectUtils.equals(mHeaderText, toolbarCustomization.mHeaderText)
                && ObjectUtils.equals(mButtonText, toolbarCustomization.mButtonText);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mBackgroundColor, mStatusBarColor, mHeaderText, mButtonText);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mBackgroundColor);
        dest.writeString(mStatusBarColor);
        dest.writeString(mHeaderText);
        dest.writeString(mButtonText);
    }
}
