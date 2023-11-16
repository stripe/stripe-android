package com.stripe.android.paymentsheet.utils

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.ui.DefaultEditPaymentMethodViewInteractor
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
        removeExecutor: PaymentMethodRemoveOperation,
        updateExecutor: PaymentMethodUpdateOperation
    ): ModifiableEditPaymentMethodViewInteractor {
        return DefaultEditPaymentMethodViewInteractor(
            initialPaymentMethod = initialPaymentMethod,
            removeExecutor = removeExecutor,
            updateExecutor = updateExecutor,
            workContext = context,
            viewStateSharingStarted = SharingStarted.Eagerly
        )
    }
}
