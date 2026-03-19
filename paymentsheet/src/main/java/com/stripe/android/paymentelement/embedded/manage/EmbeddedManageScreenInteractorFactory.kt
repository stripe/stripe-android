package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.DefaultManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import javax.inject.Inject

internal fun interface EmbeddedManageScreenInteractorFactory {
    fun createManageScreenInteractor(
        savedPaymentMethodMutator: SavedPaymentMethodMutator,
        close: (shouldInvokeRowSelectionCallback: Boolean) -> Unit,
        navigateBack: () -> Unit,
    ): ManageScreenInteractor
}

internal class DefaultEmbeddedManageScreenInteractorFactory @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val eventReporter: EventReporter,
) : EmbeddedManageScreenInteractorFactory {
    override fun createManageScreenInteractor(
        savedPaymentMethodMutator: SavedPaymentMethodMutator,
        close: (shouldInvokeRowSelectionCallback: Boolean) -> Unit,
        navigateBack: () -> Unit,
    ): ManageScreenInteractor {
        return DefaultManageScreenInteractor(
            paymentMethods = customerStateHolder.paymentMethods,
            paymentMethodMetadata = paymentMethodMetadata,
            selection = selectionHolder.selection,
            editing = savedPaymentMethodMutator.editing,
            canEdit = savedPaymentMethodMutator.canEdit,
            toggleEdit = savedPaymentMethodMutator::toggleEditing,
            onSelectPaymentMethod = {
                val savedPmSelection = PaymentSelection.Saved(it.paymentMethod)
                selectionHolder.set(savedPmSelection)
                eventReporter.onSelectPaymentOption(savedPmSelection)
                close(true)
            },
            onUpdatePaymentMethod = savedPaymentMethodMutator::updatePaymentMethod,
            navigateBack = { _ -> navigateBack() },
            defaultPaymentMethodId = savedPaymentMethodMutator.defaultPaymentMethodId,
        )
    }
}
