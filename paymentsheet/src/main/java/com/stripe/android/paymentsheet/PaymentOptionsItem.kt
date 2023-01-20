package com.stripe.android.paymentsheet

import android.content.res.Resources
import com.stripe.android.model.PaymentMethod

internal sealed class PaymentOptionsItem {
    abstract val viewType: ViewType

    object AddCard : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.AddCard
    }

    object GooglePay : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.GooglePay
    }

    object Link : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.Link
    }

    /**
     * Represents a [PaymentMethod] that is already saved and attached to the current customer.
     */
    data class SavedPaymentMethod constructor(
        val displayName: String,
        val paymentMethod: PaymentMethod,
    ) : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.SavedPaymentMethod

        fun getDescription(resources: Resources) = when (paymentMethod.type) {
            PaymentMethod.Type.Card -> resources.getString(
                R.string.card_ending_in,
                paymentMethod.card?.brand,
                paymentMethod.card?.last4
            )
            PaymentMethod.Type.SepaDebit -> resources.getString(
                R.string.bank_account_ending_in,
                paymentMethod.sepaDebit?.last4
            )
            PaymentMethod.Type.USBankAccount -> resources.getString(
                R.string.bank_account_ending_in,
                paymentMethod.usBankAccount?.last4
            )
            else -> ""
        }

        fun getRemoveDescription(resources: Resources) = resources.getString(
            R.string.stripe_paymentsheet_remove_pm,
            getDescription(resources)
        )
    }

    enum class ViewType {
        SavedPaymentMethod,
        AddCard,
        GooglePay,
        Link
    }
}
