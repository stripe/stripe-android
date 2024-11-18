package com.stripe.android.paymentsheet.ui

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.uicore.utils.combineAsStateFlow
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

    val state: StateFlow<State>

    data class State(
        val error: ResolvableString?,
        val isRemoving: Boolean,
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
    private val removeExecutor: PaymentMethodRemoveOperation,
    workContext: CoroutineContext = Dispatchers.Default,
) : UpdatePaymentMethodInteractor {
    private val coroutineScope = CoroutineScope(workContext + SupervisorJob())
    private val error = MutableStateFlow<ResolvableString?>(null)
    private val isRemoving = MutableStateFlow(false)

    private val _state = combineAsStateFlow(
        error,
        isRemoving,
    ) { error, isRemoving ->
        UpdatePaymentMethodInteractor.State(
            error = error,
            isRemoving = isRemoving,
        )
    }
    override val state = _state

    override fun handleViewAction(viewAction: UpdatePaymentMethodInteractor.ViewAction) {
        when (viewAction) {
            UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod -> {
                coroutineScope.launch {
                    error.emit(null)
                    isRemoving.emit(true)

                    val removeError = removeExecutor(displayableSavedPaymentMethod.paymentMethod)

                    isRemoving.emit(false)
                    error.emit(removeError?.stripeErrorMessage())
                }
            }
        }
    }
}
