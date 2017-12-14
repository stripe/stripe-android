package com.stripe.android.view;

import com.stripe.android.R;

enum PaymentFlowPagerEnum {

    SHIPPING_INFO(R.string.stripe_title_add_an_address, R.layout.stripe_activity_enter_shipping_info),
    SHIPPING_METHOD(R.string.stripe_title_select_shipping_method,
            R.layout.stripe_activity_select_shipping_method);

    private int mTitleResId;
    private int mLayoutResId;

    PaymentFlowPagerEnum(int titleResId, int layoutResId) {
        mTitleResId = titleResId;
        mLayoutResId = layoutResId;
    }

    int getTitleResId() {
        return mTitleResId;
    }

    int getLayoutResId() {
        return mLayoutResId;
    }

}
