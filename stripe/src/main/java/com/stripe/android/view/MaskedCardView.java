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
import com.stripe.android.model.PaymentMethod;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.view.ViewUtils.getThemeAccentColor;
import static com.stripe.android.view.ViewUtils.getThemeColorControlNormal;
import static com.stripe.android.view.ViewUtils.getThemeTextColorSecondary;

/**
 * View that displays card information without revealing the entire number, usually for
 * selection in a list. The view can be toggled to "selected" state. Colors for the selected
 * and unselected states are taken from the host Activity theme's
 * "colorAccent" and "colorControlNormal" states.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MaskedCardView extends LinearLayout {

    @PaymentMethod.Card.Brand private String mCardBrand;
    private String mLast4;
    private boolean mIsSelected;

    private AppCompatImageView mCardIconImageView;
    private AppCompatTextView mCardInformationTextView;
    private AppCompatImageView mCheckMarkImageView;

    @ColorInt private int mSelectedAlphaColorInt;
    @ColorInt private int mSelectedColorInt;
    @ColorInt private int mUnselectedColorInt;
    @ColorInt private int mUnselectedTextAlphaColorInt;
    @ColorInt private int mUnselectedTextColorInt;

    @NonNull private static final Map<String, Integer> TEMPLATE_RESOURCE_MAP = new HashMap<>();

    static {
        TEMPLATE_RESOURCE_MAP
                .put(PaymentMethod.Card.AMERICAN_EXPRESS, R.drawable.ic_amex_template_32);
        TEMPLATE_RESOURCE_MAP.put(PaymentMethod.Card.DINERS_CLUB, R.drawable.ic_diners_template_32);
        TEMPLATE_RESOURCE_MAP.put(PaymentMethod.Card.DISCOVER, R.drawable.ic_discover_template_32);
        TEMPLATE_RESOURCE_MAP.put(PaymentMethod.Card.JCB, R.drawable.ic_jcb_template_32);
        TEMPLATE_RESOURCE_MAP
                .put(PaymentMethod.Card.MASTERCARD, R.drawable.ic_mastercard_template_32);
        TEMPLATE_RESOURCE_MAP.put(PaymentMethod.Card.VISA, R.drawable.ic_visa_template_32);
        TEMPLATE_RESOURCE_MAP.put(PaymentMethod.Card.UNIONPAY, R.drawable.ic_unionpay_template_32);
        TEMPLATE_RESOURCE_MAP.put(PaymentMethod.Card.UNKNOWN, R.drawable.ic_unknown);
    }

    public MaskedCardView(Context context) {
        super(context);
        init();
    }

    public MaskedCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MaskedCardView(Context context, AttributeSet attrs, int defStyle) {
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
        updateBrandIcon();
        updateCardInformation();
    }

    void setPaymentMethod(@NonNull PaymentMethod paymentMethod) {
        mCardBrand = paymentMethod.card != null ?
                paymentMethod.card.brand : PaymentMethod.Card.UNKNOWN;
        mLast4 = paymentMethod.card != null ? paymentMethod.card.last4 : null;
        updateBrandIcon();
        updateCardInformation();
    }

    /**
     * Toggle the view from selected to unselected or vice-versa.
     */
    void toggleSelected() {
        setSelected(!mIsSelected);
    }

    @VisibleForTesting
    int[] getTextColorValues() {
        int[] colorValues = new int[4];
        colorValues[0] = mSelectedColorInt;
        colorValues[1] = mSelectedAlphaColorInt;
        colorValues[2] = mUnselectedTextColorInt;
        colorValues[3] = mUnselectedTextAlphaColorInt;
        return colorValues;
    }

    @PaymentMethod.Card.Brand
    @VisibleForTesting
    String getCardBrand() {
        return mCardBrand;
    }

    @VisibleForTesting
    String getLast4() {
        return mLast4;
    }

    private void init() {
        inflate(getContext(), R.layout.masked_card_view, this);
        setOrientation(HORIZONTAL);
        setMinimumWidth(getResources().getDimensionPixelSize(R.dimen.card_widget_min_width));
        int paddingPixels = getContext()
                .getResources().getDimensionPixelSize(R.dimen.masked_card_vertical_padding);
        setPadding(0, paddingPixels, 0, paddingPixels);

        mCardIconImageView = findViewById(R.id.masked_icon_view);
        mCardInformationTextView = findViewById(R.id.masked_card_info_view);
        mCheckMarkImageView = findViewById(R.id.masked_check_icon);

        mSelectedColorInt = getThemeAccentColor(getContext()).data;
        mUnselectedColorInt = getThemeColorControlNormal(getContext()).data;
        mUnselectedTextColorInt = getThemeTextColorSecondary(getContext()).data;
        useDefaultColorsIfThemeColorsAreInvisible();
        setLightTextColorValues();

        initializeCheckMark();
        updateCheckMark();
    }

    private void initializeCheckMark() {
        updateDrawable(R.drawable.ic_checkmark, mCheckMarkImageView, true);
    }

    private void updateBrandIcon() {
        if (mCardBrand == null || !TEMPLATE_RESOURCE_MAP.containsKey(mCardBrand)) {
            return;
        }
        @DrawableRes int iconResourceId = TEMPLATE_RESOURCE_MAP.get(mCardBrand);
        updateDrawable(iconResourceId, mCardIconImageView, false);
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

    private void updateCardInformation() {
        String brandText = PaymentMethod.Card.AMERICAN_EXPRESS.equals(mCardBrand)
                ? getResources().getString(R.string.amex_short)
                : mCardBrand;
        String normalText = getResources().getString(R.string.ending_in);
        int brandLength = brandText.length();
        int middleLength = normalText.length();
        int last4length = mLast4.length();
        @ColorInt int textColor = mIsSelected ? mSelectedColorInt : mUnselectedTextColorInt;
        @ColorInt int lightTextColor = mIsSelected
                ? mSelectedAlphaColorInt
                : mUnselectedTextAlphaColorInt;

        SpannableString str = new SpannableString(brandText + normalText + mLast4);
        str.setSpan(
                new TypefaceSpan("sans-serif-medium"),
                0,
                brandLength,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        str.setSpan(
                new ForegroundColorSpan(textColor),
                0,
                brandLength,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        str.setSpan(
                new ForegroundColorSpan(lightTextColor),
                brandLength,
                brandLength + middleLength,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        str.setSpan(
                new TypefaceSpan("sans-serif-medium"),
                brandLength + middleLength,
                brandLength + middleLength + last4length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        str.setSpan(
                new ForegroundColorSpan(textColor),
                brandLength + middleLength,
                brandLength + middleLength + last4length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mCardInformationTextView.setText(str);
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

    private void setLightTextColorValues() {
        mSelectedAlphaColorInt = ColorUtils.setAlphaComponent(mSelectedColorInt,
                getResources().getInteger(R.integer.light_text_alpha_hex));
        mUnselectedTextAlphaColorInt = ColorUtils.setAlphaComponent(mUnselectedTextColorInt,
                getResources().getInteger(R.integer.light_text_alpha_hex));
    }
}
