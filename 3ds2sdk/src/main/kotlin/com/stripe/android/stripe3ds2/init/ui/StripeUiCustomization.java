package com.stripe.android.stripe3ds2.init.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;
import com.stripe.android.stripe3ds2.utils.CustomizeUtils;
import com.stripe.android.stripe3ds2.utils.ObjectUtils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class StripeUiCustomization implements UiCustomization, Parcelable {
    public static final Creator<StripeUiCustomization> CREATOR =
            new Creator<StripeUiCustomization>() {
        @Override
        public StripeUiCustomization createFromParcel(Parcel in) {
            return new StripeUiCustomization(in);
        }

        @Override
        public StripeUiCustomization[] newArray(int size) {
            return new StripeUiCustomization[size];
        }
    };

    @Nullable private ToolbarCustomization mToolbarCustomization;
    @Nullable private LabelCustomization mLabelCustomization;
    @Nullable private TextBoxCustomization mTextBoxCustomization;
    @NonNull private final Map<ButtonType, ButtonCustomization> mDefaultButtonTypeCustomizations;
    @NonNull private final Map<String, ButtonCustomization> mCustomButtonTypeCustomization;
    @Nullable private String mAccentColor;

    public StripeUiCustomization() {
        mDefaultButtonTypeCustomizations = new EnumMap<>(ButtonType.class);
        mCustomButtonTypeCustomization = new HashMap<>();
    }

    /**
     * Attempt to create default customizations based off the given application's theme context
     *
     * @param activity The activity context to get the theme colors from
     */
    @NonNull
    public static StripeUiCustomization createWithAppTheme(@NonNull Activity activity) {
        return new StripeUiCustomization(activity);
    }

    /**
     * Create default customizations based off the given application's theme
     *
     * @param activity The activity context to get the theme colors from
     */
    private StripeUiCustomization(@NonNull Activity activity) {
        this();
        final Context actionBarContext = getThemeContext(activity,
                androidx.appcompat.R.attr.actionBarTheme);
        final String colorPrimary = getThemeColor(activity, android.R.attr.colorPrimary);
        final String colorPrimaryDark = getThemeColor(activity, android.R.attr.colorPrimaryDark);
        final String textColorPrimary =
                getThemeColor(actionBarContext, android.R.attr.textColorPrimary);
        final String textColor = getThemeColor(activity, android.R.attr.textColor);
        final String accentColor = getThemeColor(activity, android.R.attr.colorAccent);
        final String hintTextColor = getThemeColor(activity, android.R.attr.textColorHint);

        mToolbarCustomization = new StripeToolbarCustomization();
        mLabelCustomization = new StripeLabelCustomization();
        mTextBoxCustomization = new StripeTextBoxCustomization();

        if (hintTextColor != null) {
            mTextBoxCustomization.setHintTextColor(hintTextColor);
        }

        final ButtonCustomization cancelButtonCustomization = new StripeButtonCustomization();
        // for the next, submit, select, and continue button types
        final ButtonCustomization buttonCustomization = new StripeButtonCustomization();

        if (textColorPrimary != null) {
            mToolbarCustomization.setTextColor(textColorPrimary);
            cancelButtonCustomization.setTextColor(textColorPrimary);
        }

        if (colorPrimary != null) {
            mToolbarCustomization.setBackgroundColor(colorPrimary);
        }

        if (colorPrimaryDark != null) {
            mToolbarCustomization.setStatusBarColor(colorPrimaryDark);
        }

        if (textColor != null) {
            mLabelCustomization.setTextColor(textColor);
            mLabelCustomization.setHeadingTextColor(textColor);
            buttonCustomization.setTextColor(textColor);
            mTextBoxCustomization.setTextColor(textColor);
        }

        if (accentColor != null) {
            setAccentColor(accentColor);

            final ButtonCustomization resendButtonCustomization = new StripeButtonCustomization();
            resendButtonCustomization.setTextColor(accentColor);
            setButtonCustomization(resendButtonCustomization, ButtonType.RESEND);

            buttonCustomization.setBackgroundColor(accentColor);
        }

        setButtonCustomization(cancelButtonCustomization, ButtonType.CANCEL);
        setButtonCustomization(buttonCustomization, ButtonType.NEXT);
        setButtonCustomization(buttonCustomization, ButtonType.CONTINUE);
        setButtonCustomization(buttonCustomization, ButtonType.SUBMIT);
        setButtonCustomization(buttonCustomization, ButtonType.SELECT);
    }

    /**
     * Attempts to retrieve the given color attribute's value from the given context's theme
     *
     * @param context The context from which to get the color from
     * @param colorAttrResId the color attribute resource ID to retrieve
     * @return The color as a string in hex format #AARRGGBB
     */
    @Nullable
    private String getThemeColor(@NonNull Context context, @AttrRes int colorAttrResId) {
        final TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(colorAttrResId, typedValue, true)) {
            @ColorInt final int color;
            // if the typed value is a pointer to a color resource, or the actual value itself
            if (typedValue.resourceId != 0) {
                color = ContextCompat.getColor(context, typedValue.resourceId);
            } else {
                color = typedValue.data;
            }
            return CustomizeUtils.colorIntToHex(color);
        }
        return null;
    }

    /**
     * Get a context wrapped in the given theme attribute resource.
     *
     * @param activity the activity to get the theme from and wrap
     * @param themeAttrResId the theme to wrap the base context with
     * @return wrapped theme context or base context if theme resource not found
     */
    @NonNull
    private Context getThemeContext(@NonNull Activity activity, @AttrRes int themeAttrResId) {
        final TypedValue typedValue = new TypedValue();
        if (activity.getTheme().resolveAttribute(themeAttrResId, typedValue, true)) {
            return new ContextThemeWrapper(activity, typedValue.resourceId);
        }
        return activity;
    }

    private StripeUiCustomization(Parcel in) {
        mAccentColor = in.readString();
        mToolbarCustomization =
                in.readParcelable(StripeToolbarCustomization.class.getClassLoader());
        mLabelCustomization =
                in.readParcelable(StripeLabelCustomization.class.getClassLoader());
        mTextBoxCustomization =
                in.readParcelable(StripeTextBoxCustomization.class.getClassLoader());

        mDefaultButtonTypeCustomizations = new HashMap<>();
        final Bundle defaultButtonTypeCustomizationsBundle =
                in.readBundle(getClass().getClassLoader());
        if (defaultButtonTypeCustomizationsBundle != null) {
            for (String key : defaultButtonTypeCustomizationsBundle.keySet()) {
                final ButtonCustomization buttonCustomization =
                        defaultButtonTypeCustomizationsBundle.getParcelable(key);
                if (buttonCustomization != null) {
                    mDefaultButtonTypeCustomizations
                            .put(ButtonType.valueOf(key), buttonCustomization);
                }
            }
        }

        mCustomButtonTypeCustomization = new HashMap<>();
        final Bundle customButtonTypeCustomizationsBundle =
                in.readBundle(getClass().getClassLoader());
        if (customButtonTypeCustomizationsBundle != null) {
            for (String key : customButtonTypeCustomizationsBundle.keySet()) {
                final ButtonCustomization buttonCustomization =
                        customButtonTypeCustomizationsBundle.getParcelable(key);
                if (buttonCustomization != null) {
                    mCustomButtonTypeCustomization.put(key, buttonCustomization);
                }
            }
        }
    }


    @Override
    public void setButtonCustomization(@NonNull ButtonCustomization buttonCustomization,
                                       @NonNull ButtonType buttonType)
            throws InvalidInputException {
        mDefaultButtonTypeCustomizations.put(buttonType, buttonCustomization);
    }

    @Override
    public void setButtonCustomization(@NonNull ButtonCustomization buttonCustomization,
                                       @NonNull String buttonType)
            throws InvalidInputException {
        mCustomButtonTypeCustomization.put(buttonType, buttonCustomization);
    }

    @Override
    public void setToolbarCustomization(@NonNull ToolbarCustomization toolbarCustomization)
            throws InvalidInputException {
        mToolbarCustomization = toolbarCustomization;
    }

    @Override
    public void setLabelCustomization(@NonNull LabelCustomization labelCustomization)
            throws InvalidInputException {
        mLabelCustomization = labelCustomization;
    }

    @Override
    public void setTextBoxCustomization(@NonNull TextBoxCustomization textBoxCustomization)
            throws InvalidInputException {
        mTextBoxCustomization = textBoxCustomization;
    }

    @Override
    public void setAccentColor(@NonNull String accentColor) {
        mAccentColor = CustomizeUtils.requireValidColor(accentColor);
    }

    @Nullable
    @Override
    public ButtonCustomization getButtonCustomization(@NonNull ButtonType buttonType)
            throws InvalidInputException {
        return mDefaultButtonTypeCustomizations.get(buttonType);
    }

    @Nullable
    @Override
    public ButtonCustomization getButtonCustomization(@NonNull String buttonType)
            throws InvalidInputException {
        return mCustomButtonTypeCustomization.get(buttonType);
    }

    @Nullable
    @Override
    public ToolbarCustomization getToolbarCustomization() {
        return mToolbarCustomization;
    }

    @Nullable
    @Override
    public LabelCustomization getLabelCustomization() {
        return mLabelCustomization;
    }

    @Nullable
    @Override
    public TextBoxCustomization getTextBoxCustomization() {
        return mTextBoxCustomization;
    }

    @Override
    @Nullable
    public String getAccentColor() {
        return mAccentColor;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof StripeUiCustomization
                && typedEquals((StripeUiCustomization) obj));
    }

    private boolean typedEquals(@NonNull StripeUiCustomization uiCustomization) {
        return ObjectUtils.equals(mToolbarCustomization, uiCustomization.mToolbarCustomization)
                && ObjectUtils.equals(mAccentColor, uiCustomization.mAccentColor)
                && ObjectUtils.equals(mLabelCustomization, uiCustomization.mLabelCustomization)
                && ObjectUtils.equals(mTextBoxCustomization, uiCustomization.mTextBoxCustomization)
                && ObjectUtils.equals(mDefaultButtonTypeCustomizations,
                    uiCustomization.mDefaultButtonTypeCustomizations)
                && ObjectUtils.equals(mCustomButtonTypeCustomization,
                    uiCustomization.mCustomButtonTypeCustomization);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mToolbarCustomization, mAccentColor, mLabelCustomization,
                mTextBoxCustomization, mDefaultButtonTypeCustomizations,
                mCustomButtonTypeCustomization);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mAccentColor);
        dest.writeParcelable((StripeToolbarCustomization) mToolbarCustomization, 0);
        dest.writeParcelable((StripeLabelCustomization) mLabelCustomization, 0);
        dest.writeParcelable((StripeTextBoxCustomization) mTextBoxCustomization, 0);

        final Bundle defaultButtonTypeCustomizationsBundle = new Bundle();
        for (final Map.Entry<ButtonType, ButtonCustomization> entry :
                mDefaultButtonTypeCustomizations.entrySet()) {
            defaultButtonTypeCustomizationsBundle.putParcelable(entry.getKey().name(),
                    (StripeButtonCustomization) entry.getValue());
        }
        dest.writeBundle(defaultButtonTypeCustomizationsBundle);

        final Bundle customButtonTypeCustomizationsBundle = new Bundle();
        for (final Map.Entry<String, ButtonCustomization> entry :
                mCustomButtonTypeCustomization.entrySet()) {
            customButtonTypeCustomizationsBundle.putParcelable(entry.getKey(),
                    (StripeButtonCustomization) entry.getValue());
        }
        dest.writeBundle(customButtonTypeCustomizationsBundle);
    }
}
