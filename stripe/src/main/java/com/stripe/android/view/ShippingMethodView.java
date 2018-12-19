package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.stripe.android.R;
import com.stripe.android.model.ShippingMethod;

import static com.stripe.android.view.ViewUtils.getThemeAccentColor;
import static com.stripe.android.view.ViewUtils.getThemeTextColorPrimary;
import static com.stripe.android.view.ViewUtils.getThemeTextColorSecondary;

/**
 * Renders the information related to a shipping method.
 */
class ShippingMethodView extends RelativeLayout {
    private TextView mLabel;
    private TextView mDetail;
    private TextView mAmount;
    private ImageView mCheckmark;

    @ColorInt int mSelectedColorInt;
    @ColorInt int mUnselectedTextColorSecondaryInt;
    @ColorInt int mUnselectedTextColorPrimaryInt;

    ShippingMethodView(Context context) {
        super(context);
        initView();
    }

    ShippingMethodView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    ShippingMethodView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @Override
    public void setSelected(boolean selected) {
        if (selected) {
            mLabel.setTextColor(mSelectedColorInt);
            mDetail.setTextColor(mSelectedColorInt);
            mAmount.setTextColor(mSelectedColorInt);
            mCheckmark.setVisibility(VISIBLE);
        } else {
            mLabel.setTextColor(mUnselectedTextColorPrimaryInt);
            mDetail.setTextColor(mUnselectedTextColorSecondaryInt);
            mAmount.setTextColor(mUnselectedTextColorPrimaryInt);
            mCheckmark.setVisibility(INVISIBLE);
        }
    }

    private void initView() {
        inflate(getContext(), R.layout.shipping_method_view, this);
        mLabel = findViewById(R.id.tv_label_smv);
        mDetail = findViewById(R.id.tv_detail_smv);
        mAmount = findViewById(R.id.tv_amount_smv);
        mCheckmark = findViewById(R.id.iv_selected_icon);
        mSelectedColorInt = getThemeAccentColor(getContext()).data;
        mUnselectedTextColorPrimaryInt = getThemeTextColorPrimary(getContext()).data;
        mUnselectedTextColorSecondaryInt = getThemeTextColorSecondary(getContext()).data;
        useDefaultColorsIfThemeColorsAreInvisible();
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams
                .MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(Gravity.CENTER_VERTICAL);
        params.height = ViewUtils.getPxFromDp(getContext(), 72);
        setLayoutParams(params);
    }

    private void useDefaultColorsIfThemeColorsAreInvisible() {
        mSelectedColorInt =
                ViewUtils.isColorTransparent(mSelectedColorInt) ?
                        ContextCompat.getColor(getContext(), R.color.accent_color_default) :
                        mSelectedColorInt;
        mUnselectedTextColorPrimaryInt =
                ViewUtils.isColorTransparent(mUnselectedTextColorPrimaryInt) ?
                        ContextCompat.getColor(getContext(),
                                R.color.color_text_unselected_primary_default) :
                        mUnselectedTextColorPrimaryInt;
        mUnselectedTextColorSecondaryInt =
                ViewUtils.isColorTransparent(mUnselectedTextColorSecondaryInt) ?
                        ContextCompat.getColor(getContext(),
                                R.color.color_text_unselected_secondary_default) :
                        mUnselectedTextColorSecondaryInt;
    }

    void setShippingMethod(@NonNull ShippingMethod shippingMethod) {
        mLabel.setText(shippingMethod.getLabel());
        mDetail.setText(shippingMethod.getDetail());
        mAmount.setText(PaymentUtils.formatPriceStringUsingFree(
                shippingMethod.getAmount(),
                shippingMethod.getCurrency(),
                getContext().getString(R.string.price_free)));
    }
}
