package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.core.strings.orEmpty
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState

internal fun PaymentMethod.toDisplayableSavedPaymentMethod(
    paymentMethodMetadata: PaymentMethodMetadata?,
    defaultPaymentMethodId: String?,
    selectionState: SavedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Idle,
): DisplayableSavedPaymentMethod {
    return DisplayableSavedPaymentMethod.create(
        displayName = paymentMethodMetadata?.displayNameForCode(type?.code).orEmpty(),
        paymentMethod = this,
        selectionState = selectionState,
        shouldShowDefaultBadge = id == defaultPaymentMethodId,
    )
}
