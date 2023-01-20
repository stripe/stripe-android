package com.stripe.android.paymentsheet.state

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.R

internal data class WalletsContainerState(
    val showLink: Boolean = false,
    val showGooglePay: Boolean = false,
    @StringRes val dividerTextResource: Int = R.string.stripe_paymentsheet_or_pay_using,
) {
    val shouldShow: Boolean
        get() = showLink || showGooglePay
}
