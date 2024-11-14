package com.stripe.android.paymentsheet.ui

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal interface UpdatePaymentMethodInteractor {
    val isLiveMode: Boolean
    val canRemove: Boolean
    val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod
    val card: PaymentMethod.Card

    val state: StateFlow<State>

    data class State(
        val error: ResolvableString?,
    )

    fun handleViewAction(viewAction: ViewAction)

    sealed class ViewAction {
        data object RemovePaymentMethod : ViewAction()
    }
}

internal class DefaultUpdatePaymentMethodInteractor(
    override val isLiveMode: Boolean,
    override val canRemove: Boolean,
    override val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    override val card: PaymentMethod.Card,
    private val removeExecutor: PaymentMethodRemoveOperation,
    workContext: CoroutineContext = Dispatchers.Default,
) : UpdatePaymentMethodInteractor {
    private val coroutineScope = CoroutineScope(workContext + SupervisorJob())
    private val error = MutableStateFlow<ResolvableString?>(null)

    private val _state = error.mapAsStateFlow {
        UpdatePaymentMethodInteractor.State(error = it)
    }
    override val state = _state

    override fun handleViewAction(viewAction: UpdatePaymentMethodInteractor.ViewAction) {
        when (viewAction) {
            UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod -> {
                coroutineScope.launch {
                    error.emit(null)

                    val removeError = removeExecutor(displayableSavedPaymentMethod.paymentMethod)

                    error.emit(removeError?.stripeErrorMessage())
                }
            }
        }
    }
}
