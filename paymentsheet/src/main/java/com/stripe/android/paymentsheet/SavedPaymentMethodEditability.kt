package com.stripe.android.paymentsheet

import com.stripe.android.core.utils.DateUtils
import com.stripe.android.model.PaymentMethod

internal fun PaymentMethod.isModifiable(
    canUpdateCardExpiryAndBillingDetails: Boolean,
    canChangeCbc: Boolean,
): Boolean {
    val card = editableSavedCard() ?: return false
    return canUpdateCardExpiryAndBillingDetails ||
        (canChangeCbc && card.isExpired().not() && hasMultipleNetworks())
}

internal fun PaymentMethod.hasMultipleNetworks(): Boolean {
    val card = editableSavedCard() ?: return false
    return card.networks?.available?.let { it.size > 1 } ?: false
}

internal fun PaymentMethod.Card.isExpired(): Boolean {
    val cardExpiryMonth = expiryMonth
    val cardExpiryYear = expiryYear
    return cardExpiryMonth != null && cardExpiryYear != null &&
        !DateUtils.isExpiryDataValid(
            expiryMonth = cardExpiryMonth,
            expiryYear = cardExpiryYear,
        )
}

private fun PaymentMethod.editableSavedCard(): PaymentMethod.Card? {
    return card.takeIf {
        type == PaymentMethod.Type.Card && isLinkPaymentMethod.not()
    }
}
