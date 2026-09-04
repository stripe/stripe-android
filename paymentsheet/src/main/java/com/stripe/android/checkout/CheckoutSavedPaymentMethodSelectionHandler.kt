package com.stripe.android.checkout

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.VerticalSavedPaymentMethodSelectionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)
internal class CheckoutSavedPaymentMethodSelectionHandler @Inject constructor(
    private val checkoutController: CheckoutController,
    private val immediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : VerticalSavedPaymentMethodSelectionHandler {
    private val _state = MutableStateFlow<VerticalSavedPaymentMethodSelectionHandler.State>(
        VerticalSavedPaymentMethodSelectionHandler.State.Idle
    )
    override val state = _state.asStateFlow()

    override fun select(selection: PaymentSelection.Saved) {
        if (_state.value is VerticalSavedPaymentMethodSelectionHandler.State.Selecting) return

        _state.value = VerticalSavedPaymentMethodSelectionHandler.State.Selecting(selection)
        coroutineScope.launch {
            checkoutController.selectSavedPaymentMethod(selection).fold(
                onSuccess = {
                    _state.value = VerticalSavedPaymentMethodSelectionHandler.State.Idle
                    immediateActionHandler.invoke()
                },
                onFailure = { error ->
                    _state.value = VerticalSavedPaymentMethodSelectionHandler.State.Failed(error)
                },
            )
        }
    }
}
