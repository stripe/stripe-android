package com.stripe.android.view

import android.support.annotation.LayoutRes
import android.support.annotation.StringRes
import com.stripe.android.R

internal enum class PaymentFlowPagerEnum(
    @param:StringRes @field:StringRes @get:StringRes val titleResId: Int,
    @param:LayoutRes @field:LayoutRes @get:LayoutRes val layoutResId: Int
) {
    SHIPPING_INFO(R.string.title_add_an_address, R.layout.activity_enter_shipping_info),
    SHIPPING_METHOD(R.string.title_select_shipping_method, R.layout.activity_select_shipping_method)
}
