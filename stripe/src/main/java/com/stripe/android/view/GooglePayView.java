package com.stripe.android.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.stripe.android.R;
import com.stripe.android.model.Card;
import com.stripe.android.model.CustomerSource;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.view.ViewUtils.getThemeAccentColor;
import static com.stripe.android.view.ViewUtils.getThemeColorControlNormal;
import static com.stripe.android.view.ViewUtils.getThemeTextColorSecondary;

/**
 * View that "Pay with Google" payment method, usually for
 * selection in a list. The view can be toggled to "selected" state. Colors for the selected
 * and unselected states are taken from the host Activity theme's
 * "colorAccent" and "colorControlNormal" states.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GooglePayView extends LinearLayout {

    private boolean mIsSelected;

    private AppCompatTextView mGoogleInformationTextView;
    private AppCompatImageView mCheckMarkImageView;

    @ColorInt int mSelectedColorInt;
    @ColorInt int mUnselectedColorInt;
    @ColorInt int mUnselectedTextColorInt;

    public GooglePayView(Context context) {
        super(context);
        init();
    }

    public GooglePayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GooglePayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public boolean isSelected() {
        return mIsSelected;
    }

    @Override
    public void setSelected(boolean selected) {
        mIsSelected = selected;
        updateCheckMark();
        updateGoogleInformation();
    }

    /**
     * Toggle the view from selected to unselected or vice-versa.
     */
    void toggleSelected() {
        setSelected(!mIsSelected);
    }

    private void init() {
        inflate(getContext(), R.layout.pay_with_google_view, this);
        setOrientation(HORIZONTAL);
        setMinimumWidth(getResources().getDimensionPixelSize(R.dimen.card_widget_min_width));
        int paddingPixels = getContext()
                .getResources().getDimensionPixelSize(R.dimen.masked_card_vertical_padding);
        setPadding(0, paddingPixels, 0, paddingPixels);

        mGoogleInformationTextView = findViewById(R.id.google_info_view);
        mCheckMarkImageView = findViewById(R.id.google_check_icon);

        mSelectedColorInt = getThemeAccentColor(getContext()).data;
        mUnselectedColorInt = getThemeColorControlNormal(getContext()).data;
        mUnselectedTextColorInt = getThemeTextColorSecondary(getContext()).data;
        useDefaultColorsIfThemeColorsAreInvisible();

        initializeCheckMark();
        updateCheckMark();
        updateGoogleInformation();
    }

    private void initializeCheckMark() {
        updateDrawable(R.drawable.ic_checkmark, mCheckMarkImageView, true);
    }

    private void updateDrawable(
            @DrawableRes int resourceId,
            @NonNull ImageView imageView,
            boolean isCheckMark) {
        final Drawable icon = ContextCompat.getDrawable(getContext(), resourceId);
        @ColorInt int tintColor = mIsSelected || isCheckMark ?
                mSelectedColorInt : mUnselectedColorInt;
        Drawable compatIcon = DrawableCompat.wrap(icon);
        DrawableCompat.setTint(compatIcon.mutate(), tintColor);
        imageView.setImageDrawable(compatIcon);
    }

    private void updateGoogleInformation() {
        String googleText = getResources().getString(R.string.pay_with_google);
        int googleLength = googleText.length();
        @ColorInt int textColor = mIsSelected ? mSelectedColorInt : mUnselectedTextColorInt;

        SpannableString str = new SpannableString(googleText);
        str.setSpan(
                new TypefaceSpan("sans-serif-medium"),
                0,
                googleLength,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        str.setSpan(
                new ForegroundColorSpan(textColor),
                0,
                googleLength,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mGoogleInformationTextView.setText(str);
    }

    private void updateCheckMark() {
        if (mIsSelected) {
            mCheckMarkImageView.setVisibility(View.VISIBLE);
        } else {
            mCheckMarkImageView.setVisibility(View.INVISIBLE);
        }
    }

    private void useDefaultColorsIfThemeColorsAreInvisible() {
        mSelectedColorInt = ViewUtils.isColorTransparent(mSelectedColorInt) ?
                ContextCompat.getColor(getContext(), R.color.accent_color_default) :
                mSelectedColorInt;
        mUnselectedColorInt = ViewUtils.isColorTransparent(mUnselectedColorInt) ?
                ContextCompat.getColor(getContext(), R.color.control_normal_color_default) :
                mUnselectedColorInt;
        mUnselectedTextColorInt = ViewUtils.isColorTransparent(mUnselectedTextColorInt) ?
                ContextCompat.getColor(getContext(), R.color.color_text_secondary_default) :
                mUnselectedTextColorInt;
    }

}
