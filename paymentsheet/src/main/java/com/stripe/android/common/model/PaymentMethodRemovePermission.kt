package com.stripe.android.common.model

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R

internal enum class PaymentMethodRemovePermission(private val removeMessageId: Int?) {
    Full(removeMessageId = null),
    Partial(removeMessageId = R.string.stripe_paymentsheet_remove_partial_description),
    None(removeMessageId = null);

    fun removeMessage(merchantDisplayName: String) = removeMessageId?.let {
        resolvableString(it, merchantDisplayName)
    }
}
