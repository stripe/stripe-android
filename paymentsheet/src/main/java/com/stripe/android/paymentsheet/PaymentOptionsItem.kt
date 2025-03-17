package com.stripe.android.paymentsheet

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentMethod

internal sealed class PaymentOptionsItem {

    abstract val viewType: PaymentOptionsItemViewType
    abstract val isEnabledDuringEditing: Boolean

    object AddCard : PaymentOptionsItem() {
        override val viewType: PaymentOptionsItemViewType = PaymentOptionsItemViewType.AddCard
        override val isEnabledDuringEditing: Boolean = false
    }

    object GooglePay : PaymentOptionsItem() {
        override val viewType: PaymentOptionsItemViewType = PaymentOptionsItemViewType.GooglePay
        override val isEnabledDuringEditing: Boolean = false
    }

    object Link : PaymentOptionsItem() {
        override val viewType: PaymentOptionsItemViewType = PaymentOptionsItemViewType.Link
        override val isEnabledDuringEditing: Boolean = false
    }

    /**
     * Represents a [PaymentMethod] that is already saved and attached to the current customer.
     */
    data class SavedPaymentMethod(
        val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    ) : PaymentOptionsItem() {
        override val viewType: PaymentOptionsItemViewType = PaymentOptionsItemViewType.SavedPaymentMethod

        val displayName = displayableSavedPaymentMethod.displayName
        val paymentMethod = displayableSavedPaymentMethod.paymentMethod
        val isModifiable: Boolean by lazy { displayableSavedPaymentMethod.isModifiable() }

        override val isEnabledDuringEditing: Boolean = true
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class PaymentOptionsItemViewType {
    SavedPaymentMethod,
    AddCard,
    GooglePay,
    Link,
}

internal val PaymentOptionsItem.key: String
    get() {
        val paymentMethodId = (this as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod?.id
        return listOfNotNull(viewType, paymentMethodId).joinToString("-")
    }
