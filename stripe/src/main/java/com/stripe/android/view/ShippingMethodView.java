package com.stripe.android.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    @NonNull private final TextView mLabel;
    @NonNull private final TextView mDetail;
    @NonNull private final TextView mAmount;
    @NonNull private final ImageView mCheckmark;

    @ColorInt private final int mSelectedColorInt;
    @ColorInt private final int mUnselectedTextColorSecondaryInt;
    @ColorInt private final int mUnselectedTextColorPrimaryInt;

    ShippingMethodView(@NonNull Context context) {
        this(context, null);
    }

    ShippingMethodView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    ShippingMethodView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.shipping_method_view, this);
        mLabel = findViewById(R.id.tv_label_smv);
        mDetail = findViewById(R.id.tv_detail_smv);
        mAmount = findViewById(R.id.tv_amount_smv);
        mCheckmark = findViewById(R.id.iv_selected_icon);

        final int rawSelectedColorInt = getThemeAccentColor(context).data;
        final int rawUselectedTextColorPrimaryInt = getThemeTextColorPrimary(context).data;
        final int rawUnselectedTextColorSecondaryInt = getThemeTextColorSecondary(context).data;
        mSelectedColorInt =
                ViewUtils.isColorTransparent(rawSelectedColorInt) ?
                        ContextCompat.getColor(context, R.color.accent_color_default) :
                        rawSelectedColorInt;
        mUnselectedTextColorPrimaryInt =
                ViewUtils.isColorTransparent(rawUselectedTextColorPrimaryInt) ?
                        ContextCompat.getColor(context,
                                R.color.color_text_unselected_primary_default) :
                        rawUselectedTextColorPrimaryInt;
        mUnselectedTextColorSecondaryInt =
                ViewUtils.isColorTransparent(rawUnselectedTextColorSecondaryInt) ?
                        ContextCompat.getColor(context,
                                R.color.color_text_unselected_secondary_default) :
                        rawUnselectedTextColorSecondaryInt;

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
        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams
                .MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            params.addRule(Gravity.CENTER_VERTICAL);
        }
        final int height = getResources()
                .getDimensionPixelSize(R.dimen.shipping_method_view_height);
        params.height = ViewUtils.getPxFromDp(getContext(), height);
        setLayoutParams(params);
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
