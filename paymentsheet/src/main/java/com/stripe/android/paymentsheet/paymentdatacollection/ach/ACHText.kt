package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.content.Context
import com.stripe.android.paymentsheet.R

/**
 * Temporary hack to get mandate text to display properly until translations are fixed
 */
internal object ACHText {

    fun getContinueMandateText(
        context: Context,
        merchantName: String,
        isSaveForFutureUseSelected: Boolean,
        isSetupFlow: Boolean,
    ): String {
        val text = if (isSaveForFutureUseSelected || isSetupFlow) {
            context.getString(R.string.stripe_paymentsheet_ach_save_mandate, merchantName)
        } else {
            context.getString(R.string.stripe_paymentsheet_ach_continue_mandate)
        }

        return text.replace(
            oldValue = "<terms>",
            newValue = "<a href=\"https://stripe.com/ach-payments/authorization\">",
        ).replace(
            oldValue = "</terms>",
            newValue = "</a>",
        )
    }
}
