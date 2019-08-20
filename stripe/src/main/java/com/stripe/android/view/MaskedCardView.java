package com.stripe.android.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
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
import java.util.Objects;

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

    @NonNull private final AppCompatImageView mCardIconImageView;
    @NonNull private final AppCompatTextView mCardInformationTextView;
    @NonNull private final AppCompatImageView mCheckMarkImageView;

    @NonNull private static final Map<String, Integer> ICON_RESOURCE_MAP = new HashMap<>();
    @NonNull private static final Map<String, Integer> BRAND_RESOURCE_MAP = new HashMap<>();

    @NonNull private final ThemeConfig mThemeConfig;

    static {
        ICON_RESOURCE_MAP
                .put(PaymentMethod.Card.Brand.AMERICAN_EXPRESS, R.drawable.ic_amex_template_32);
        ICON_RESOURCE_MAP
                .put(PaymentMethod.Card.Brand.DINERS_CLUB, R.drawable.ic_diners_template_32);
        ICON_RESOURCE_MAP
                .put(PaymentMethod.Card.Brand.DISCOVER, R.drawable.ic_discover_template_32);
        ICON_RESOURCE_MAP.put(PaymentMethod.Card.Brand.JCB, R.drawable.ic_jcb_template_32);
        ICON_RESOURCE_MAP
                .put(PaymentMethod.Card.Brand.MASTERCARD, R.drawable.ic_mastercard_template_32);
        ICON_RESOURCE_MAP.put(PaymentMethod.Card.Brand.VISA, R.drawable.ic_visa_template_32);
        ICON_RESOURCE_MAP
                .put(PaymentMethod.Card.Brand.UNIONPAY, R.drawable.ic_unionpay_template_32);
        ICON_RESOURCE_MAP.put(PaymentMethod.Card.Brand.UNKNOWN, R.drawable.ic_unknown);

        BRAND_RESOURCE_MAP.put(PaymentMethod.Card.Brand.AMERICAN_EXPRESS, R.string.amex_short);
        BRAND_RESOURCE_MAP.put(PaymentMethod.Card.Brand.DINERS_CLUB, R.string.diners_club);
        BRAND_RESOURCE_MAP.put(PaymentMethod.Card.Brand.DISCOVER, R.string.discover);
        BRAND_RESOURCE_MAP.put(PaymentMethod.Card.Brand.JCB, R.string.jcb);
        BRAND_RESOURCE_MAP.put(PaymentMethod.Card.Brand.MASTERCARD, R.string.mastercard);
        BRAND_RESOURCE_MAP.put(PaymentMethod.Card.Brand.VISA, R.string.visa);
        BRAND_RESOURCE_MAP.put(PaymentMethod.Card.Brand.UNIONPAY, R.string.unionpay);
        BRAND_RESOURCE_MAP.put(PaymentMethod.Card.Brand.UNKNOWN, R.string.unknown);
    }

    public MaskedCardView(@NonNull Context context) {
        this(context, null);
    }

    public MaskedCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaskedCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        inflate(getContext(), R.layout.masked_card_view, this);
        mCardIconImageView = findViewById(R.id.masked_icon_view);
        mCardInformationTextView = findViewById(R.id.masked_card_info_view);
        mCheckMarkImageView = findViewById(R.id.masked_check_icon);

        mThemeConfig = new ThemeConfig(context);

        initializeCheckMark();
        updateCheckMark();
    }

    @Override
    public boolean isSelected() {
        return mIsSelected;
    }

    @Override
    public void setSelected(boolean selected) {
        mIsSelected = selected;
        updateCheckMark();
        updateUi();
    }

    void setPaymentMethod(@NonNull PaymentMethod paymentMethod) {
        mCardBrand = paymentMethod.card != null ?
                paymentMethod.card.brand : PaymentMethod.Card.Brand.UNKNOWN;
        mLast4 = paymentMethod.card != null ? paymentMethod.card.last4 : "";
        updateUi();
    }

    private void updateUi() {
        updateBrandIcon();
        mCardInformationTextView.setText(createDisplayString());
    }

    /**
     * Toggle the view from selected to unselected or vice-versa.
     */
    void toggleSelected() {
        setSelected(!mIsSelected);
    }

    @NonNull
    @VisibleForTesting
    int[] getTextColorValues() {
        return mThemeConfig.textColorValues;
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

    private void initializeCheckMark() {
        updateDrawable(R.drawable.ic_checkmark, mCheckMarkImageView, true);
    }

    private void updateBrandIcon() {
        if (mCardBrand == null || !ICON_RESOURCE_MAP.containsKey(mCardBrand)) {
            return;
        }
        @DrawableRes final int iconResourceId =
                Objects.requireNonNull(ICON_RESOURCE_MAP.get(mCardBrand));
        updateDrawable(iconResourceId, mCardIconImageView, false);
    }

    private void updateDrawable(
            @DrawableRes int resourceId,
            @NonNull ImageView imageView,
            boolean isCheckMark) {
        final Drawable icon = DrawableCompat.wrap(
                Objects.requireNonNull(ContextCompat.getDrawable(getContext(), resourceId))
        );
        DrawableCompat.setTint(
                icon.mutate(),
                mThemeConfig.getTintColor(mIsSelected || isCheckMark)
        );
        imageView.setImageDrawable(icon);
    }

    @NonNull
    private SpannableString createDisplayString() {
        final String brandText;
        if (BRAND_RESOURCE_MAP.containsKey(mCardBrand)) {
            brandText = getResources().getString(
                    Objects.requireNonNull(BRAND_RESOURCE_MAP.get(mCardBrand)));
        } else {
            brandText = getResources().getString(R.string.unknown);
        }
        final String cardEndingIn = getResources().getString(R.string.ending_in, brandText, mLast4);
        final int totalLength = cardEndingIn.length();
        final int brandLength = brandText.length();
        final int last4length = mLast4.length();
        final int last4Start = totalLength - last4length;
        @ColorInt final int textColor = mThemeConfig.getTextColor(mIsSelected);
        @ColorInt final int lightTextColor = mThemeConfig.getTextAlphaColor(mIsSelected);

        final SpannableString displayString = new SpannableString(cardEndingIn);

        // style brand
        displayString.setSpan(
                new TypefaceSpan("sans-serif-medium"),
                0,
                brandLength,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        displayString.setSpan(
                new ForegroundColorSpan(textColor),
                0,
                brandLength,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // style "ending in"
        displayString.setSpan(
                new ForegroundColorSpan(lightTextColor),
                brandLength,
                last4Start,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // style last 4
        displayString.setSpan(
                new TypefaceSpan("sans-serif-medium"),
                last4Start,
                totalLength,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        displayString.setSpan(
                new ForegroundColorSpan(textColor),
                last4Start,
                totalLength,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return displayString;
    }

    private void updateCheckMark() {
        if (mIsSelected) {
            mCheckMarkImageView.setVisibility(View.VISIBLE);
        } else {
            mCheckMarkImageView.setVisibility(View.INVISIBLE);
        }
    }
}
