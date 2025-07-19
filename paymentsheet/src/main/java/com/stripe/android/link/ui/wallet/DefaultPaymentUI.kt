package com.stripe.android.link.ui.wallet

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.stripe.android.model.CardBrand
import com.stripe.android.model.DisplayablePaymentDetails
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.getCardBrandIconForVerticalMode
import kotlinx.parcelize.Parcelize

/**
 * UI representation of default payment details rendered in [com.stripe.android.link.ui.LinkButton]
 * and [LinkInline2FASection].
 */
@Immutable
@Parcelize
internal data class DefaultPaymentUI(
    @DrawableRes val paymentIconRes: Int,
    val last4: String
) : Parcelable

internal fun DisplayablePaymentDetails.toDefaultPaymentUI(
    enableDefaultValuesInECE: Boolean
): DefaultPaymentUI? {
    // do not render default payment details in ECE if the feature is disabled
    if (!enableDefaultValuesInECE) return null
    // do not render default payment details if there's no last4.
    if (last4 == null) return null

    val paymentIcon = when (defaultPaymentType) {
        "CARD" -> CardBrand.fromCode(defaultCardBrand).getCardBrandIconForVerticalMode()
        "BANK_ACCOUNT" -> R.drawable.stripe_link_bank_outlined
        else -> null
    }
    // do not render default payment details if icon is not available
    if (paymentIcon == null) return null

    return DefaultPaymentUI(
        paymentIconRes = paymentIcon,
        last4 = requireNotNull(last4)
    )
}
