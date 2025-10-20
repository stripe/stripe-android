package com.stripe.android.link.ui.wallet

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.stripe.android.model.CardBrand
import com.stripe.android.model.DisplayablePaymentDetails
import com.stripe.android.paymentsheet.ui.getCardBrandIconForVerticalMode
import kotlinx.parcelize.Parcelize

/**
 * UI representation of default payment details rendered in [com.stripe.android.link.ui.LinkButton]
 * and [LinkInline2FASection].
 */
@Immutable
@Parcelize
internal data class DefaultPaymentUI(
    val paymentType: PaymentType,
    val last4: String
) : Parcelable {

    @Immutable
    @Parcelize
    internal sealed interface PaymentType : Parcelable {
        @Parcelize
        data class Card(@DrawableRes val iconRes: Int) : PaymentType

        @Parcelize
        data class BankAccount(val bankIconCode: String?) : PaymentType
    }
}

internal fun DisplayablePaymentDetails.toDefaultPaymentUI(
    enableDefaultValuesInECE: Boolean
): DefaultPaymentUI? {
    // do not render default payment details in ECE if the feature is disabled
    if (!enableDefaultValuesInECE) return null
    // do not render default payment details if there's no last4.
    if (last4 == null) return null

    val paymentType = when (defaultPaymentType) {
        "CARD" -> {
            val cardIcon = CardBrand.fromCode(defaultCardBrand).getCardBrandIconForVerticalMode()
            DefaultPaymentUI.PaymentType.Card(cardIcon)
        }
        "BANK_ACCOUNT" -> {
            // Note: DisplayablePaymentDetails doesn't include bankIconCode, so we pass null
            // This will result in the generic bank icon being used
            DefaultPaymentUI.PaymentType.BankAccount(bankIconCode = null)
        }
        else -> null
    }

    // do not render default payment details if payment type is not supported
    if (paymentType == null) return null

    return DefaultPaymentUI(
        paymentType = paymentType,
        last4 = requireNotNull(last4)
    )
}
