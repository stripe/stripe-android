package com.stripe.android.view

import androidx.annotation.StringRes
import com.stripe.android.R

internal enum class PaymentFlowPage(
    @param:StringRes @field:StringRes @get:StringRes val titleResId: Int
) {
    ShippingInfo(R.string.title_add_an_address),
    ShippingMethod(R.string.title_select_shipping_method)
}
