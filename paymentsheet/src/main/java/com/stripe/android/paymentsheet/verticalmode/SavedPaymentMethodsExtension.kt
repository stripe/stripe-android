package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod

internal fun PaymentMethod.toDisplayableSavedPaymentMethod(
    paymentMethodMetadata: PaymentMethodMetadata?,
    defaultPaymentMethodId: String?,
    isSelectionPending: Boolean = false,
    selectionError: ResolvableString? = null,
): DisplayableSavedPaymentMethod {
    return DisplayableSavedPaymentMethod.create(
        displayName = paymentMethodMetadata?.displayNameForCode(type?.code).orEmpty(),
        paymentMethod = this,
        isSelectionPending = isSelectionPending,
        selectionError = selectionError,
        shouldShowDefaultBadge = id == defaultPaymentMethodId,
    )
}
