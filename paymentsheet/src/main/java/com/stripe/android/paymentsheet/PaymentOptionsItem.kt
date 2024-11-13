package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod

internal sealed class PaymentOptionsItem {

    abstract val viewType: ViewType
    abstract fun isEnabledDuringEditing(useUpdatePaymentMethodScreen: Boolean): Boolean

    object AddCard : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.AddCard
        override fun isEnabledDuringEditing(useUpdatePaymentMethodScreen: Boolean): Boolean = false
    }

    object GooglePay : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.GooglePay
        override fun isEnabledDuringEditing(useUpdatePaymentMethodScreen: Boolean): Boolean = false
    }

    object Link : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.Link
        override fun isEnabledDuringEditing(useUpdatePaymentMethodScreen: Boolean): Boolean = false
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

        override fun isEnabledDuringEditing(useUpdatePaymentMethodScreen: Boolean): Boolean {
            // When using the update payment method screen, we should allow users to access the update payment method
            // screen to see their card details.
            return useUpdatePaymentMethodScreen || isModifiable || canRemovePaymentMethods
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
