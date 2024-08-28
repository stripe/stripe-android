package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod

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
        val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
        private val canRemovePaymentMethods: Boolean,
    ) : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.SavedPaymentMethod

        val displayName = displayableSavedPaymentMethod.displayName
        val paymentMethod = displayableSavedPaymentMethod.paymentMethod
        val isModifiable: Boolean by lazy { displayableSavedPaymentMethod.isModifiable() }

        override val isEnabledDuringEditing: Boolean by lazy {
            isModifiable || canRemovePaymentMethods
        }
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
