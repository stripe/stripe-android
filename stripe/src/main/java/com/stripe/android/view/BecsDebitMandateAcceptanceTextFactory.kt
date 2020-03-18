package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.text.Html
import com.stripe.android.R

/**
 * A class to create BECS Debit Mandate Agreement text for the [BecsDebitWidget].
 */
class BecsDebitMandateAcceptanceTextFactory(
    private val context: Context
) {
    fun create(merchantName: String): CharSequence {
        val mandateAcceptanceText = context.getString(
            R.string.becs_mandate_acceptance,
            merchantName
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(mandateAcceptanceText, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(mandateAcceptanceText)
        }
    }
}
