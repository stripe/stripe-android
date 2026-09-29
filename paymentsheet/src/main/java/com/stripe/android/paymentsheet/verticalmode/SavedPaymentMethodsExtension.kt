package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.core.strings.orEmpty
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState

internal fun PaymentMethod.toDisplayableSavedPaymentMethod(
    paymentMethodMetadata: PaymentMethodMetadata?,
    defaultPaymentMethodId: String?,
    selectionState: SavedPaymentMethodSelectionState,
): DisplayableSavedPaymentMethod {
    return DisplayableSavedPaymentMethod.create(
        displayName = paymentMethodMetadata?.displayNameForCode(type?.code).orEmpty(),
        paymentMethod = this,
        isSelectionPending = selectionState is SavedPaymentMethodSelectionState.Pending &&
            selectionState.paymentMethodId == id,
        selectionError = (selectionState as? SavedPaymentMethodSelectionState.Failed)
            ?.takeIf { it.paymentMethodId == id }
            ?.error,
        shouldShowDefaultBadge = id == defaultPaymentMethodId,
    )
}
