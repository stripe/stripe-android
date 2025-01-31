package com.stripe.android.paymentsheet.utils

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.R as StripeUiCoreR

internal fun buyButtonLabel(
    amount: Amount?,
    primaryButtonLabel: String?,
    isForPaymentIntent: Boolean
): ResolvableString {
    return primaryButtonLabel?.resolvableString ?: run {
        if (isForPaymentIntent) {
            val fallback = R.string.stripe_paymentsheet_pay_button_label.resolvableString
            amount?.buildPayButtonLabel() ?: fallback
        } else {
            StripeUiCoreR.string.stripe_setup_button_label.resolvableString
        }
    }
}

internal fun continueButtonLabel(primaryButtonLabel: String?): ResolvableString {
    return primaryButtonLabel?.resolvableString
        ?: StripeUiCoreR.string.stripe_continue_button_label.resolvableString
}
