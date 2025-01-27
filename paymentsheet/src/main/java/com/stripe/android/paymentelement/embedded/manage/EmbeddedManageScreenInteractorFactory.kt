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
import javax.inject.Provider

internal interface EmbeddedManageScreenInteractorFactory {
    fun createManageScreenInteractor(): ManageScreenInteractor
}

internal class DefaultEmbeddedManageScreenInteractorFactory @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val savedPaymentMethodMutator: SavedPaymentMethodMutator,
    private val eventReporter: EventReporter,
    private val manageNavigatorProvider: Provider<ManageNavigator>,
) : EmbeddedManageScreenInteractorFactory {
    override fun createManageScreenInteractor(): ManageScreenInteractor {
        return DefaultManageScreenInteractor(
            paymentMethods = customerStateHolder.paymentMethods,
            paymentMethodMetadata = paymentMethodMetadata,
            selection = selectionHolder.selection,
            editing = savedPaymentMethodMutator.editing,
            canEdit = savedPaymentMethodMutator.canEdit,
            toggleEdit = savedPaymentMethodMutator::toggleEditing,
            providePaymentMethodName = savedPaymentMethodMutator.providePaymentMethodName,
            onSelectPaymentMethod = {
                val savedPmSelection = PaymentSelection.Saved(it.paymentMethod)
                selectionHolder.set(savedPmSelection)
                eventReporter.onSelectPaymentOption(savedPmSelection)
                manageNavigatorProvider.get().performAction(ManageNavigator.Action.Back)
            },
            onUpdatePaymentMethod = savedPaymentMethodMutator::updatePaymentMethod,
            navigateBack = {
                manageNavigatorProvider.get().performAction(ManageNavigator.Action.Back)
            },
            defaultPaymentMethodId = savedPaymentMethodMutator.defaultPaymentMethodId,
        )
    }
}
