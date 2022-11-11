package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.core.ResolvableString
import com.stripe.android.core.resolvableString
import com.stripe.android.paymentsheet.R

/**
 * Temporary hack to get mandate text to display properly until translations are fixed
 */
internal object ACHText {
    fun getContinueMandateText(): ResolvableString {
        return resolvableString(
            id = R.string.stripe_paymentsheet_ach_continue_mandate,
            transform = {
                it.replace(
                    oldValue = "<terms>",
                    newValue = "<a href=\"https://stripe.com/ach-payments/authorization\">",
                ).replace(
                    oldValue = "</terms>",
                    newValue = "</a>",
                )
            }
        )
    }
}
