package com.stripe.android.checkout

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSavedPaymentMethodSelectionHandler
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.billingDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)
internal class CheckoutSavedPaymentMethodSelectionHandler @Inject constructor(
    private val stateHolder: CheckoutControllerStateHolder,
    private val operationCoordinator: CheckoutOperationCoordinator,
    private val taxRegionUpdater: CheckoutSessionTaxRegionUpdater,
    private val stateLoader: CheckoutStateLoader,
    private val immediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : EmbeddedSavedPaymentMethodSelectionHandler {
    private val _pendingSelection = MutableStateFlow<PaymentSelection.Saved?>(null)
    override val pendingSelection = _pendingSelection.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    override val error = _error.asStateFlow()

    override fun select(selection: PaymentSelection.Saved) {
        if (_pendingSelection.value != null) return

        _pendingSelection.value = selection
        _error.value = null
        coroutineScope.launch {
            operationCoordinator.runMutation {
                runCatching {
                    val state = requireNotNull(stateHolder.state)
                    val address = selection.billingDetails?.address?.toCheckoutAddress()
                    val response = if (address != null) {
                        taxRegionUpdater.updateServerStateIfNeeded(
                            checkoutSessionResponse = state.checkoutSessionResponse,
                            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                            address = address,
                        ).getOrThrow()
                    } else {
                        state.checkoutSessionResponse
                    }
                    stateLoader.reload(
                        state.copy(
                            checkoutSessionResponse = response,
                            paymentSelection = selection,
                        )
                    )
                }
            }.fold(
                onSuccess = {
                    _pendingSelection.value = null
                    _error.value = null
                    immediateActionHandler.invoke()
                },
                onFailure = { error ->
                    _pendingSelection.value = null
                    _error.value = error
                },
            )
        }
    }
}
