package com.stripe.android.paymentsheet.utils

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.ui.DefaultEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PaymentMethodRemoveOperation
import com.stripe.android.paymentsheet.ui.PaymentMethodUpdateOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlin.coroutines.CoroutineContext

internal class FakeEditPaymentMethodInteractorFactory(
    private val context: CoroutineContext = Dispatchers.Main
) : ModifiableEditPaymentMethodViewInteractor.Factory {
    override fun create(
        initialPaymentMethod: PaymentMethod,
        eventHandler: (EditPaymentMethodViewInteractor.Event) -> Unit,
        removeExecutor: PaymentMethodRemoveOperation,
        updateExecutor: PaymentMethodUpdateOperation,
        displayName: String
    ): ModifiableEditPaymentMethodViewInteractor {
        return DefaultEditPaymentMethodViewInteractor(
            initialPaymentMethod = initialPaymentMethod,
            eventHandler = eventHandler,
            removeExecutor = removeExecutor,
            updateExecutor = updateExecutor,
            displayName = displayName,
            workContext = context,
            viewStateSharingStarted = SharingStarted.Eagerly
        )
    }
}
