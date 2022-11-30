package com.stripe.android.ui.core

import android.content.res.Resources
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.uicore.format.CurrencyFormatter
import kotlinx.parcelize.Parcelize

/**
 * This class represents the long value amount to charge and the currency code of the amount.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class Amount(val value: Long, val currencyCode: String) : Parcelable {

    /**
     * Builds a localized label in the format "Pay $10.99".
     */
    fun buildPayButtonLabel(resources: Resources) =
        resources.getString(
            R.string.stripe_pay_button_amount,
            CurrencyFormatter.format(value, currencyCode)
        )
}
