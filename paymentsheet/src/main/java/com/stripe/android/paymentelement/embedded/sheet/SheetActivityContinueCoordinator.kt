package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface SheetActivityContinueCoordinator {
    fun onContinue()
}

internal class DefaultSheetActivityContinueCoordinator @Inject constructor(
    private val taxRegionUpdater: SheetTaxRegionUpdater,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val stateHolder: SheetActivityStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val launchMode: EmbeddedLaunchMode,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : SheetActivityContinueCoordinator {

    override fun onContinue() {
        val selection = selectionHolder.selection.value
        val taxRegionUpdate = taxRegionUpdater.prepareUpdate(paymentMethodMetadata, selection)
        if (taxRegionUpdate == null) {
            stateHolder.setResult(createResult(selection, checkoutSessionResponse = null))
            return
        }

        coroutineScope.launch {
            stateHolder.updateProcessing(true)
            taxRegionUpdate().fold(
                onSuccess = { response ->
                    stateHolder.setResult(createResult(selection, response))
                },
                onFailure = { error ->
                    stateHolder.updateProcessing(false)
                    stateHolder.updateError(error.stripeErrorMessage())
                },
            )
        }
    }

    private fun createResult(
        selection: PaymentSelection?,
        checkoutSessionResponse: CheckoutSessionResponse?,
    ): EmbeddedActivityResult.Complete {
        return EmbeddedActivityResult.Complete(
            selection = selection,
            previousNewSelections = selectionHolder.previousNewSelections,
            hasBeenConfirmed = false,
            customerState = customerStateHolder.customer.value,
            checkoutSessionResponse = checkoutSessionResponse,
            shouldInvokeSelectionCallback = false,
            launchMode = launchMode,
        )
    }
}
