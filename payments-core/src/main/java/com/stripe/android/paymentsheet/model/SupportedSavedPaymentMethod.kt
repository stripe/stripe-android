package com.stripe.android.paymentsheet.model

import android.content.res.Resources
import androidx.annotation.DrawableRes
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod

/**
 * Enum defining all types of saved payment method supported on Payment Sheet.
 *
 * These are fetched from the
 * [PaymentMethods API endpoint](https://stripe.com/docs/api/payment_methods/list) for returning
 * customers.
 */
internal enum class SupportedSavedPaymentMethod(
    val type: PaymentMethod.Type
) {
    Card(PaymentMethod.Type.Card),
    SepaDebit(PaymentMethod.Type.SepaDebit);

    companion object {
        internal fun fromCode(code: String) =
            values().firstOrNull { it.type.code == code }

        internal fun isSupported(paymentMethod: PaymentMethod) =
            when (paymentMethod.type) {
                PaymentMethod.Type.Card -> paymentMethod.card != null
                PaymentMethod.Type.SepaDebit -> paymentMethod.sepaDebit != null
                else -> false
            }
    }
}

@DrawableRes
internal fun PaymentMethod.getSavedPaymentMethodIcon(): Int? = when (type) {
    PaymentMethod.Type.Card -> card?.brand?.getSavedPaymentMethodIcon()
        ?: R.drawable.stripe_ic_paymentsheet_card_unknown
    PaymentMethod.Type.SepaDebit -> R.drawable.stripe_ic_paymentsheet_pm_sepa_debit
    else -> null
}

@DrawableRes
internal fun CardBrand.getSavedPaymentMethodIcon(): Int = when (this) {
    CardBrand.Visa -> R.drawable.stripe_ic_paymentsheet_card_visa
    CardBrand.AmericanExpress -> R.drawable.stripe_ic_paymentsheet_card_amex
    CardBrand.Discover -> R.drawable.stripe_ic_paymentsheet_card_discover
    CardBrand.JCB -> R.drawable.stripe_ic_paymentsheet_card_jcb
    CardBrand.DinersClub -> R.drawable.stripe_ic_paymentsheet_card_dinersclub
    CardBrand.MasterCard -> R.drawable.stripe_ic_paymentsheet_card_mastercard
    CardBrand.UnionPay -> R.drawable.stripe_ic_paymentsheet_card_unionpay
    CardBrand.Unknown -> R.drawable.stripe_ic_paymentsheet_card_unknown
}

internal fun PaymentMethod.getLabel(resources: Resources): String? = when (type) {
    PaymentMethod.Type.Card -> createCardLabel(resources, card?.last4)
    PaymentMethod.Type.SepaDebit -> resources.getString(
        R.string.paymentsheet_payment_method_item_card_number,
        sepaDebit?.last4
    )
    else -> null
}

internal fun createCardLabel(resources: Resources, last4: String?): String {
    return last4?.let {
        resources.getString(
            R.string.paymentsheet_payment_method_item_card_number,
            last4
        )
    }.orEmpty()
}
