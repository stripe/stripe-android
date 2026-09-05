package com.stripe.android.checkout

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.VerticalPaymentSelectionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)
internal class CheckoutPaymentSelectionHandler @Inject constructor(
    private val checkoutController: CheckoutController,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val immediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : VerticalPaymentSelectionHandler {
    private var isSelectingSavedPaymentMethod = false

    override fun select(selection: PaymentSelection, isUserInput: Boolean) {
        when (selection) {
            is PaymentSelection.Saved -> selectSavedPaymentMethod(selection)
            else -> {
                selectionHolder.setSelection(selection)
                onSelectionComplete()
            }
        }
    }

    override fun onSelectionComplete() {
        immediateActionHandler.invoke()
    }

    private fun selectSavedPaymentMethod(selection: PaymentSelection.Saved) {
        if (isSelectingSavedPaymentMethod) return

        isSelectingSavedPaymentMethod = true
        coroutineScope.launch {
            val result = checkoutController.selectSavedPaymentMethod(selection)
            isSelectingSavedPaymentMethod = false
            result.onSuccess { onSelectionComplete() }
        }
    }
}
