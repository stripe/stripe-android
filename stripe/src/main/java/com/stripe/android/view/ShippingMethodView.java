package com.stripe.android.view;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.model.ShippingMethod;

import static com.stripe.android.view.ViewUtils.getThemeAccentColor;
import static com.stripe.android.view.ViewUtils.getThemeTextColorSecondary;
import static com.stripe.android.view.ViewUtils.isColorTransparent;

/**
 * Renders the information related to a shipping method.
 */
class ShippingMethodView extends RelativeLayout {

    private ShippingMethod mShippingMethod;

    private TextView mLabel;
    private TextView mDetail;
    private TextView mAmount;
    private ImageView mCheckmark;

    @ColorInt int mSelectedColorInt;
    @ColorInt int mUnselectedTextColorInt;

    public ShippingMethodView(Context context) {
        super(context);
        initView();
    }

    public ShippingMethodView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public ShippingMethodView(Context context, AttributeSet attrs, int defStyleAttr) {
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
            mLabel.setTextColor(mUnselectedTextColorInt);
            mDetail.setTextColor(mUnselectedTextColorInt);
            mAmount.setTextColor(mUnselectedTextColorInt);
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
        mUnselectedTextColorInt =  getThemeTextColorSecondary(getContext()).data;
        if (isColorTransparent(mUnselectedTextColorInt)) {
            mUnselectedTextColorInt = Color.BLACK;
        }
    }

    void setShippingMethod(@NonNull ShippingMethod shippingMethod) {
        mShippingMethod = shippingMethod;
        mLabel.setText(mShippingMethod.getLabel());
        mDetail.setText(mShippingMethod.getDetail());
        mAmount.setText(
                PaymentUtils.getPriceString(getContext(),
                        mShippingMethod.getAmount(),
                PaymentConfiguration.getInstance().getCurrency()));
    }
}
