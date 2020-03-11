package com.stripe.android.view

import android.content.Context
import com.stripe.android.R

/**
 * A class to create BECS Debit Mandate Agreement text for the [BecsDebitWidget].
 */
class BecsDebitMandateAcceptanceFactory(
    private val context: Context
) {
    fun create(merchantName: String): String {
        return context.getString(R.string.becs_mandate_acceptance, merchantName)
    }
}
