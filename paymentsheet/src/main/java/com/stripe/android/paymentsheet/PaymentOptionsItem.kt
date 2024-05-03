package com.stripe.android.paymentsheet

import android.content.res.Resources
import com.stripe.android.model.PaymentMethod
import com.stripe.android.R as StripeR

internal sealed class PaymentOptionsItem {

    abstract val viewType: ViewType
    abstract val isEnabledDuringEditing: Boolean

    object AddCard : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.AddCard
        override val isEnabledDuringEditing: Boolean = false
    }

    object GooglePay : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.GooglePay
        override val isEnabledDuringEditing: Boolean = false
    }

    object Link : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.Link
        override val isEnabledDuringEditing: Boolean = false
    }

    /**
     * Represents a [PaymentMethod] that is already saved and attached to the current customer.
     */
    data class SavedPaymentMethod(
        val displayName: String,
        val paymentMethod: PaymentMethod,
        val isCbcEligible: Boolean = false
    ) : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.SavedPaymentMethod
        override val isEnabledDuringEditing: Boolean = true

        val isModifiable: Boolean by lazy {
            val hasMultipleNetworks = paymentMethod.card?.networks?.available?.let { available ->
                available.size > 1
            } ?: false

            isCbcEligible && hasMultipleNetworks
        }

        fun getDescription(resources: Resources) = when (paymentMethod.type) {
            PaymentMethod.Type.Card -> resources.getString(
                StripeR.string.stripe_card_ending_in,
                paymentMethod.card?.brand,
                paymentMethod.card?.last4
            )
            PaymentMethod.Type.SepaDebit -> resources.getString(
                R.string.stripe_bank_account_ending_in,
                paymentMethod.sepaDebit?.last4
            )
            PaymentMethod.Type.USBankAccount -> resources.getString(
                R.string.stripe_bank_account_ending_in,
                paymentMethod.usBankAccount?.last4
            )
            else -> ""
        }

        fun getModifyDescription(resources: Resources) = resources.getString(
            R.string.stripe_paymentsheet_modify_pm,
            getDescription(resources)
        )

        fun getRemoveDescription(resources: Resources) = resources.getString(
            R.string.stripe_paymentsheet_remove_pm,
            getDescription(resources)
        )
    }

    enum class ViewType {
        SavedPaymentMethod,
        AddCard,
        GooglePay,
        Link,
    }
}

internal val PaymentOptionsItem.key: String
    get() {
        val paymentMethodId = (this as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod?.id
        return listOfNotNull(viewType, paymentMethodId).joinToString("-")
    }
