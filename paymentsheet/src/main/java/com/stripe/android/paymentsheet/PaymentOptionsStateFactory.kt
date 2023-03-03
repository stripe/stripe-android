package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection

internal data class PaymentOptionsState(
    val items: List<PaymentOptionsItem> = emptyList(),
    val selectedIndex: Int = -1,
) {
    val selectedItem: PaymentOptionsItem?
        get() = items.getOrNull(selectedIndex)
}

internal object PaymentOptionsStateFactory {

    fun create(
        paymentMethods: List<PaymentMethod>,
        showGooglePay: Boolean,
        showLink: Boolean,
        currentSelection: PaymentSelection? = null,
        nameProvider: (PaymentMethodCode?) -> String,
    ): PaymentOptionsState {
        val items = listOfNotNull(
            PaymentOptionsItem.AddCard,
            PaymentOptionsItem.GooglePay.takeIf { showGooglePay },
            PaymentOptionsItem.Link.takeIf { showLink },
        ) + paymentMethods.map {
            PaymentOptionsItem.SavedPaymentMethod(
                displayName = nameProvider(it.type?.code),
                paymentMethod = it,
            )
        }

        val currentSelectionIndex = currentSelection?.let {
            items.findSelectedPosition(it)
        } ?: -1

        return PaymentOptionsState(
            items = items,
            selectedIndex = currentSelectionIndex,
        )
    }
}


/**
 * Find the index of [paymentSelection] in the current items. Return -1 if not found.
 */
private fun List<PaymentOptionsItem>.findSelectedPosition(paymentSelection: PaymentSelection): Int {
    return indexOfFirst { item ->
        when (paymentSelection) {
            is PaymentSelection.GooglePay -> item is PaymentOptionsItem.GooglePay
            is PaymentSelection.Link -> item is PaymentOptionsItem.Link
            is PaymentSelection.Saved -> {
                when (item) {
                    is PaymentOptionsItem.SavedPaymentMethod -> {
                        paymentSelection.paymentMethod.id == item.paymentMethod.id
                    }
                    else -> false
                }
            }
            is PaymentSelection.New -> false
        }
    }
}

internal fun PaymentOptionsItem.toPaymentSelection(): PaymentSelection? {
    return when (this) {
        is PaymentOptionsItem.AddCard -> null
        is PaymentOptionsItem.GooglePay -> PaymentSelection.GooglePay
        is PaymentOptionsItem.Link -> PaymentSelection.Link
        is PaymentOptionsItem.SavedPaymentMethod -> PaymentSelection.Saved(paymentMethod)
    }
}
