package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.content.Context
import com.stripe.android.paymentsheet.R

/**
 * Temporary hack to get mandate text to display properly until translations are fixed
 */
object ACHText {
    fun getContinueMandateText(context: Context): String {
        return context.getString(
            R.string.stripe_paymentsheet_ach_continue_mandate
        ).replace(
            "<terms>",
            "<a href=\"https://stripe.com/ach-payments/authorization\">"
        ).replace("</terms>", "</a>")
    }

    fun getSaveMandateText(context: Context, merchantName: String): String {
        return context.getString(
            R.string.stripe_paymentsheet_ach_save_mandate
        ).replace("%@", merchantName)
    }

    fun getMicrodepositText(context: Context, merchantName: String): String {
        return context.getString(
            R.string.stripe_paymentsheet_microdeposit
        ).replace("%@", merchantName)
    }
}
