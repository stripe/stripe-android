package com.stripe.android.paymentsheet

import com.stripe.android.core.utils.DateUtils
import com.stripe.android.model.PaymentMethod

internal fun PaymentMethod.isModifiable(
    canUpdateCardPaymentMethodDetails: Boolean,
    canUpdateCardBrandChoice: Boolean,
    isCbcEligible: Boolean,
): Boolean {
    val card = editableSavedCard() ?: return false

    return canUpdateCardPaymentMethodDetails ||
        (canUpdateCardBrandChoice && card.isExpired().not() && canChangeCbc(isCbcEligible))
}

internal fun PaymentMethod.canChangeCbc(isCbcEligible: Boolean): Boolean {
    val card = editableSavedCard() ?: return false
    val hasMultipleNetworks = card.networks?.available?.let { available ->
        available.size > 1
    } ?: false

    return isCbcEligible && hasMultipleNetworks
}

internal fun PaymentMethod.Card.isExpired(): Boolean {
    val cardExpiryMonth = expiryMonth
    val cardExpiryYear = expiryYear
    // If the card's expiration dates are missing, we can't conclude that it is expired, so we don't want to
    // show the user an expired card error.
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
