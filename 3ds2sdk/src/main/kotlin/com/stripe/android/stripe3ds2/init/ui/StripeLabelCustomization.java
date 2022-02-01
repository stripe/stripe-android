package com.stripe.android.stripe3ds2.init.ui;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;
import com.stripe.android.stripe3ds2.utils.CustomizeUtils;
import com.stripe.android.stripe3ds2.utils.ObjectUtils;

public final class StripeLabelCustomization extends BaseCustomization
        implements LabelCustomization, Parcelable {

    public static final Creator<StripeLabelCustomization> CREATOR =
            new Creator<StripeLabelCustomization>() {
                @Override
                public StripeLabelCustomization createFromParcel(Parcel in) {
                    return new StripeLabelCustomization(in);
                }

                @Override
                public StripeLabelCustomization[] newArray(int size) {
                    return new StripeLabelCustomization[size];
                }
            };

    @Nullable private String mHeadingTextColor;
    @Nullable private String mHeadingTextFontName;
    private int mHeadingTextFontSize;

    public StripeLabelCustomization() {
        super();
    }

    private StripeLabelCustomization(@NonNull Parcel in) {
        super(in);
        mHeadingTextColor = in.readString();
        mHeadingTextFontName = in.readString();
        mHeadingTextFontSize = in.readInt();
    }

    @Override
    public void setHeadingTextColor(@NonNull String hexColorCode) throws InvalidInputException {
        mHeadingTextColor = CustomizeUtils.requireValidColor(hexColorCode);
    }

    @Override
    public void setHeadingTextFontName(@NonNull String fontName) throws InvalidInputException {
        mHeadingTextFontName = CustomizeUtils.requireValidString(fontName);
    }

    @Override
    public void setHeadingTextFontSize(int fontSize) throws InvalidInputException {
        mHeadingTextFontSize = CustomizeUtils.requireValidFontSize(fontSize);
    }

    @Nullable
    @Override
    public String getHeadingTextColor() {
        return mHeadingTextColor;
    }

    @Nullable
    @Override
    public String getHeadingTextFontName() {
        return mHeadingTextFontName;
    }

    @Override
    public int getHeadingTextFontSize() {
        return mHeadingTextFontSize;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof StripeLabelCustomization
                && typedEquals((StripeLabelCustomization) obj));
    }

    private boolean typedEquals(@NonNull StripeLabelCustomization labelCustomization) {
        return ObjectUtils.equals(mHeadingTextColor, labelCustomization.mHeadingTextColor)
                && ObjectUtils.equals(mHeadingTextFontName, labelCustomization.mHeadingTextFontName)
                && mHeadingTextFontSize == labelCustomization.mHeadingTextFontSize;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mHeadingTextColor, mHeadingTextFontName, mHeadingTextFontSize);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mHeadingTextColor);
        dest.writeString(mHeadingTextFontName);
        dest.writeInt(mHeadingTextFontSize);
    }
}
