package com.stripe.android.paymentsheet

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentMethod

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PaymentOptionsItem {

    abstract val viewType: ViewType
    abstract val isEnabledDuringEditing: Boolean

    internal object AddCard : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.AddCard
        override val isEnabledDuringEditing: Boolean = false
    }

    internal object GooglePay : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.GooglePay
        override val isEnabledDuringEditing: Boolean = false
    }

    internal object Link : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.Link
        override val isEnabledDuringEditing: Boolean = false
    }

    /**
     * Represents a [PaymentMethod] that is already saved and attached to the current customer.
     */
    internal data class SavedPaymentMethod(
        val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    ) : PaymentOptionsItem() {
        override val viewType: ViewType = ViewType.SavedPaymentMethod

        val displayName = displayableSavedPaymentMethod.displayName
        val paymentMethod = displayableSavedPaymentMethod.paymentMethod

        fun isModifiable(canUpdatePaymentMethod: Boolean): Boolean {
            return displayableSavedPaymentMethod.isModifiable(canUpdatePaymentMethod)
        }

        override val isEnabledDuringEditing: Boolean = true
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
