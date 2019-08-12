package com.stripe.android.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import java.util.Objects;

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

    @NonNull private final AppCompatImageView mCardIconImageView;
    @NonNull private final AppCompatTextView mCardInformationTextView;
    @NonNull private final AppCompatImageView mCheckMarkImageView;

    @ColorInt private final int mSelectedAlphaColorInt;
    @ColorInt private final int mSelectedColorInt;
    @ColorInt private final int mUnselectedColorInt;
    @ColorInt private final int mUnselectedTextAlphaColorInt;
    @ColorInt private final int mUnselectedTextColorInt;
    @NonNull private final int[] mTextColorValues;

    @NonNull private static final Map<String, Integer> ICON_RESOURCE_MAP = new HashMap<>();
    @NonNull private static final Map<String, Integer> BRAND_RESOURCE_MAP = new HashMap<>();

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

        mSelectedColorInt = determineColor(
                getThemeAccentColor(getContext()).data,
                R.color.accent_color_default
        );
        mUnselectedColorInt = determineColor(
                getThemeColorControlNormal(getContext()).data,
                R.color.control_normal_color_default
        );
        mUnselectedTextColorInt = determineColor(
                getThemeTextColorSecondary(getContext()).data,
                R.color.color_text_secondary_default
        );

        mSelectedAlphaColorInt = ColorUtils.setAlphaComponent(mSelectedColorInt,
                getResources().getInteger(R.integer.light_text_alpha_hex));
        mUnselectedTextAlphaColorInt = ColorUtils.setAlphaComponent(mUnselectedTextColorInt,
                getResources().getInteger(R.integer.light_text_alpha_hex));

        mTextColorValues = new int[]{
                mSelectedColorInt,
                mSelectedAlphaColorInt,
                mUnselectedTextColorInt,
                mUnselectedTextAlphaColorInt

        };

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
        return mTextColorValues;
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
        final Drawable icon = Objects.requireNonNull(
                ContextCompat.getDrawable(getContext(), resourceId));
        @ColorInt int tintColor = mIsSelected || isCheckMark ?
                mSelectedColorInt : mUnselectedColorInt;
        final Drawable compatIcon = DrawableCompat.wrap(icon);
        DrawableCompat.setTint(compatIcon.mutate(), tintColor);
        imageView.setImageDrawable(compatIcon);
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
        @ColorInt final int textColor = mIsSelected ? mSelectedColorInt : mUnselectedTextColorInt;
        @ColorInt final int lightTextColor = mIsSelected
                ? mSelectedAlphaColorInt
                : mUnselectedTextAlphaColorInt;

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

    @ColorInt
    private int determineColor(@ColorInt int defaultColor, @ColorRes int colorIfTransparent) {
        return ViewUtils.isColorTransparent(defaultColor) ?
                ContextCompat.getColor(getContext(), colorIfTransparent) :
                defaultColor;
    }
}
