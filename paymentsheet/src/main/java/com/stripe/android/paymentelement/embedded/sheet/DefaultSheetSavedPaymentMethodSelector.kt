package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.manage.EmbeddedSavedPaymentMethodSelector
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import javax.inject.Inject

internal class DefaultSheetSavedPaymentMethodSelector @Inject constructor(
    private val taxRegionUpdater: SheetTaxRegionUpdater,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val sheetActivityStateHolder: SheetActivityStateHolder,
) : EmbeddedSavedPaymentMethodSelector {
    override suspend fun select(selection: PaymentSelection.Saved): Result<Unit> {
        val paymentMethodId = selection.paymentMethod.id
        val update = taxRegionUpdater.prepareUpdate(paymentMethodMetadata, selection)
        val response = update?.let {
            sheetActivityStateHolder.updateSavedPaymentMethodSelectionState(
                SavedPaymentMethodSelectionState.Pending(paymentMethodId),
            )
            it().getOrElse { error ->
                sheetActivityStateHolder.updateSavedPaymentMethodSelectionState(
                    SavedPaymentMethodSelectionState.Failed(error.stripeErrorMessage()),
                )
                return Result.failure(error)
            }
        }

        sheetActivityStateHolder.setCheckoutSessionResponse(response)
        selectionHolder.setSelection(selection)
        sheetActivityStateHolder.updateSavedPaymentMethodSelectionState(SavedPaymentMethodSelectionState.Idle)
        return Result.success(Unit)
    }
}
