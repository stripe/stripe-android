package com.stripe.android.paymentsheet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection

internal data class PaymentOptionsState(
    val items: List<PaymentOptionsItem> = emptyList(),
    val selectedItem: PaymentOptionsItem? = null,
)

internal object PaymentOptionsStateFactory {

    fun createPaymentOptionsList(
        paymentMethods: List<PaymentMethod>,
        showGooglePay: Boolean,
        showLink: Boolean,
        nameProvider: (PaymentMethodCode?) -> ResolvableString,
        isCbcEligible: Boolean,
        defaultPaymentMethodId: String?
    ): List<PaymentOptionsItem> {
        return listOfNotNull(
            PaymentOptionsItem.AddCard,
            PaymentOptionsItem.GooglePay.takeIf { showGooglePay },
            PaymentOptionsItem.Link.takeIf { showLink }
        ) + paymentMethods.map {
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = nameProvider(it.type?.code),
                    paymentMethod = it,
                    isCbcEligible = isCbcEligible,
                    shouldShowDefaultBadge = it.id != null && it.id == defaultPaymentMethodId
                ),
            )
        }
    }

    fun getSelectedItem(
        items: List<PaymentOptionsItem>,
        currentSelection: PaymentSelection?,
    ): PaymentOptionsItem? {
        return currentSelection?.let {
            items.findSelectedItem(it)
        }
    }

    fun create(
        paymentMethods: List<PaymentMethod>,
        showGooglePay: Boolean,
        showLink: Boolean,
        currentSelection: PaymentSelection?,
        nameProvider: (PaymentMethodCode?) -> ResolvableString,
        isCbcEligible: Boolean,
        defaultPaymentMethodId: String?
    ): PaymentOptionsState {
        val items = createPaymentOptionsList(
            paymentMethods = paymentMethods,
            showGooglePay = showGooglePay,
            showLink = showLink,
            nameProvider = nameProvider,
            isCbcEligible = isCbcEligible,
            defaultPaymentMethodId = defaultPaymentMethodId
        )

        val selectedItem = getSelectedItem(items, currentSelection)

        return PaymentOptionsState(
            items = items,
            selectedItem = selectedItem,
        )
    }
}

/**
 * Find the item matching [paymentSelection] in the current items. Return -1 if not found.
 */
private fun List<PaymentOptionsItem>.findSelectedItem(paymentSelection: PaymentSelection): PaymentOptionsItem? {
    return firstOrNull { item ->
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
            is PaymentSelection.ExternalPaymentMethod -> false
        }
    }
}

internal fun PaymentOptionsItem.toPaymentSelection(): PaymentSelection? {
    return when (this) {
        is PaymentOptionsItem.AddCard -> null
        is PaymentOptionsItem.GooglePay -> PaymentSelection.GooglePay
        is PaymentOptionsItem.Link -> PaymentSelection.Link()
        is PaymentOptionsItem.SavedPaymentMethod -> PaymentSelection.Saved(paymentMethod)
    }
}
