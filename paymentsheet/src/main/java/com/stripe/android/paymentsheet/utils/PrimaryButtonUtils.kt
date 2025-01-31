package com.stripe.android.paymentsheet.utils

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.Amount

internal fun buyButtonLabel(
    amount: Amount?,
    primaryButtonLabel: String?,
    isProcessingPayment: Boolean
): ResolvableString {
    return primaryButtonLabel?.resolvableString ?: run {
        if (isProcessingPayment) {
            val fallback = R.string.stripe_paymentsheet_pay_button_label.resolvableString
            amount?.buildPayButtonLabel() ?: fallback
        } else {
            com.stripe.android.ui.core.R.string.stripe_setup_button_label.resolvableString
        }
    }
}

internal fun continueButtonLabel(primaryButtonLabel: String?): ResolvableString {
    return primaryButtonLabel?.resolvableString
        ?: com.stripe.android.ui.core.R.string.stripe_continue_button_label.resolvableString
}
