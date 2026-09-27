package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import javax.inject.Inject

internal interface SheetSavedPaymentMethodSelectionCoordinator {
    suspend fun select(selection: PaymentSelection.Saved): Result<Unit>
}

internal class DefaultSheetSavedPaymentMethodSelectionCoordinator @Inject constructor(
    private val taxRegionUpdater: SheetTaxRegionUpdater,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val stateHolder: SheetActivityStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val linkAccountHolder: LinkAccountHolder,
    private val launchMode: EmbeddedLaunchMode,
) : SheetSavedPaymentMethodSelectionCoordinator {
    override suspend fun select(selection: PaymentSelection.Saved): Result<Unit> {
        val update = taxRegionUpdater.prepareUpdate(paymentMethodMetadata, selection)
        val response = update?.let { it().getOrElse { error -> return Result.failure(error) } }

        selectionHolder.setSelection(selection)
        stateHolder.setResult(
            EmbeddedActivityResult.Complete(
                selection = selection,
                previousNewSelections = selectionHolder.previousNewSelections,
                hasBeenConfirmed = false,
                customerState = customerStateHolder.customer.value,
                linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
                checkoutSessionResponse = response,
                shouldInvokeSelectionCallback = true,
                launchMode = launchMode,
            )
        )
        return Result.success(Unit)
    }
}
