package com.stripe.android.paymentsheet

import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection

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
        initialSelection: SavedSelection,
        currentSelection: PaymentSelection? = null,
    ): PaymentOptionsState {
        val items = listOfNotNull(
            PaymentOptionsItem.AddCard,
            PaymentOptionsItem.GooglePay.takeIf { showGooglePay },
            PaymentOptionsItem.Link.takeIf { showLink }
        ) + sortedPaymentMethods(paymentMethods, initialSelection).map {
            PaymentOptionsItem.SavedPaymentMethod(it)
        }

        val currentSelectionIndex = currentSelection?.let {
            items.findSelectedPosition(it)
        }.takeIf { it != -1 }

        val initialSelectionIndex = items.findInitialSelectedPosition(initialSelection)

        return PaymentOptionsState(
            items = items,
            selectedIndex = currentSelectionIndex ?: initialSelectionIndex,
        )
    }

    private fun sortedPaymentMethods(
        paymentMethods: List<PaymentMethod>,
        savedSelection: SavedSelection
    ): List<PaymentMethod> {
        val primaryPaymentMethodIndex = when (savedSelection) {
            is SavedSelection.PaymentMethod -> {
                paymentMethods.indexOfFirst {
                    it.id == savedSelection.id
                }
            }
            else -> -1
        }
        return if (primaryPaymentMethodIndex != -1) {
            val mutablePaymentMethods = paymentMethods.toMutableList()
            mutablePaymentMethods.removeAt(primaryPaymentMethodIndex)
                .also { primaryPaymentMethod ->
                    mutablePaymentMethods.add(0, primaryPaymentMethod)
                }
            mutablePaymentMethods
        } else {
            paymentMethods
        }
    }
}

/**
 * The initial selection position follows this prioritization:
 * 1. The index of [PaymentOptionsItem.SavedPaymentMethod] if it matches the [SavedSelection]
 * 2. The index of [PaymentOptionsItem.GooglePay] if it exists
 * 3. The index of the first [PaymentOptionsItem.SavedPaymentMethod]
 * 4. None (-1)
 */
internal fun List<PaymentOptionsItem>.findInitialSelectedPosition(
    savedSelection: SavedSelection?
): Int {
    return listOfNotNull(
        // saved selection
        indexOfFirst { item ->
            val b = when (savedSelection) {
                SavedSelection.GooglePay -> item is PaymentOptionsItem.GooglePay
                SavedSelection.Link -> item is PaymentOptionsItem.Link
                is SavedSelection.PaymentMethod -> {
                    when (item) {
                        is PaymentOptionsItem.SavedPaymentMethod -> {
                            savedSelection.id == item.paymentMethod.id
                        }
                        else -> false
                    }
                }
                SavedSelection.None -> false
                else -> false
            }
            b
        }.takeIf { it != -1 },

        // Google Pay
        indexOfFirst { it is PaymentOptionsItem.GooglePay }.takeIf { it != -1 },

        // Link
        indexOfFirst { it is PaymentOptionsItem.Link }.takeIf { it != -1 },

        // the first payment method
        indexOfFirst { it is PaymentOptionsItem.SavedPaymentMethod }.takeIf { it != -1 }
    ).firstOrNull() ?: RecyclerView.NO_POSITION
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
