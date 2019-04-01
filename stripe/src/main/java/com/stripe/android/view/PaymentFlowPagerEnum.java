package com.stripe.android.view;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;

import com.stripe.android.R;

enum PaymentFlowPagerEnum {

    SHIPPING_INFO(R.string.title_add_an_address, R.layout.activity_enter_shipping_info),
    SHIPPING_METHOD(R.string.title_select_shipping_method,
            R.layout.activity_select_shipping_method);

    @StringRes private final int mTitleResId;
    @LayoutRes private final int mLayoutResId;

    PaymentFlowPagerEnum(@StringRes int titleResId, @LayoutRes int layoutResId) {
        mTitleResId = titleResId;
        mLayoutResId = layoutResId;
    }

    @StringRes
    int getTitleResId() {
        return mTitleResId;
    }

    @LayoutRes
    int getLayoutResId() {
        return mLayoutResId;
    }
}
