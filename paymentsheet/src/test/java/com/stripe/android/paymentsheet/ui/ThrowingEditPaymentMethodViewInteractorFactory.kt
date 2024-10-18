package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethod

internal object ThrowingEditPaymentMethodViewInteractorFactory : ModifiableEditPaymentMethodViewInteractor.Factory {
    override fun create(
        initialPaymentMethod: PaymentMethod,
        eventHandler: (EditPaymentMethodViewInteractor.Event) -> Unit,
        removeExecutor: PaymentMethodRemoveOperation,
        updateExecutor: PaymentMethodUpdateOperation,
        displayName: ResolvableString,
        canRemove: Boolean,
        isLiveMode: Boolean,
        cardBrandFilter: CardBrandFilter
    ): ModifiableEditPaymentMethodViewInteractor {
        throw AssertionError("Not expected.")
    }
}
