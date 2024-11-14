package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.ViewActionRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeUpdatePaymentMethodInteractor(
    override val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    override val canRemove: Boolean,
    val viewActionRecorder: ViewActionRecorder<UpdatePaymentMethodInteractor.ViewAction>?,
    initialState: UpdatePaymentMethodInteractor.State,
) : UpdatePaymentMethodInteractor {
    override val isLiveMode: Boolean = false
    override val card: PaymentMethod.Card = displayableSavedPaymentMethod.paymentMethod.card!!
    override val state: StateFlow<UpdatePaymentMethodInteractor.State> = MutableStateFlow(initialState)

    override fun handleViewAction(viewAction: UpdatePaymentMethodInteractor.ViewAction) {
        viewActionRecorder?.record(viewAction)
    }
}