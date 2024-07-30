package com.stripe.android.stripe3ds2.init.ui;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;
import com.stripe.android.stripe3ds2.utils.CustomizeUtils;

public abstract class BaseCustomization implements Customization, Parcelable {
    @Nullable private String mTextFontName;
    @Nullable private String mTextColor;
    private int mTextFontSize;

    BaseCustomization() {

    }

    BaseCustomization(@NonNull Parcel in) {
        mTextFontName = in.readString();
        mTextColor = in.readString();
        mTextFontSize = in.readInt();
    }

    @Override
    public void setTextFontName(@NonNull String textFontName) throws InvalidInputException {
        mTextFontName = CustomizeUtils.requireValidString(textFontName);
    }

    @Override
    public void setTextColor(@NonNull String textColor) throws InvalidInputException {
        mTextColor = CustomizeUtils.requireValidColor(textColor);
    }

    @Override
    public void setTextFontSize(int textFontSize) throws InvalidInputException {
        mTextFontSize = CustomizeUtils.requireValidFontSize(textFontSize);
    }

    @Nullable
    @Override
    public String getTextFontName() {
        return mTextFontName;
    }

    @Nullable
    @Override
    public String getTextColor() {
        return mTextColor;
    }

    @Override
    public int getTextFontSize() {
        return mTextFontSize;
    }

    @CallSuper
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTextFontName);
        dest.writeString(mTextColor);
        dest.writeInt(mTextFontSize);
    }
}
