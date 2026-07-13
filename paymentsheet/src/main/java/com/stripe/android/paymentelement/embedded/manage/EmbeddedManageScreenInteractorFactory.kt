package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.DefaultManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import javax.inject.Inject
import javax.inject.Provider

internal fun interface EmbeddedManageScreenInteractorFactory {
    fun createManageScreenInteractor(): ManageScreenInteractor
}

internal class DefaultEmbeddedManageScreenInteractorFactory @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val savedPaymentMethodMutator: SavedPaymentMethodMutator,
    private val eventReporter: EventReporter,
    private val embeddedNavigatorProvider: Provider<EmbeddedNavigator>,
    private val launchMode: EmbeddedLaunchMode,
) : EmbeddedManageScreenInteractorFactory {
    override fun createManageScreenInteractor(): ManageScreenInteractor {
        return DefaultManageScreenInteractor(
            paymentMethods = customerStateHolder.paymentMethods,
            paymentMethodMetadata = paymentMethodMetadata,
            selection = selectionHolder.selection,
            editing = savedPaymentMethodMutator.editing,
            canEdit = savedPaymentMethodMutator.canEdit,
            toggleEdit = savedPaymentMethodMutator::toggleEditing,
            onSelectPaymentMethod = {
                val savedPmSelection = PaymentSelection.Saved(it.paymentMethod)
                selectionHolder.setSelection(savedPmSelection)
                eventReporter.onSelectPaymentOption(savedPmSelection)
                val action = when (launchMode) {
                    is EmbeddedLaunchMode.PaymentOptions -> EmbeddedNavigator.Action.Back
                    is EmbeddedLaunchMode.Manage,
                    is EmbeddedLaunchMode.Form -> EmbeddedNavigator.Action.Close(
                        shouldInvokeRowSelectionCallback = true
                    )
                }
                embeddedNavigatorProvider.get().performAction(action)
            },
            onUpdatePaymentMethod = savedPaymentMethodMutator::updatePaymentMethod,
            navigateBack = {
                embeddedNavigatorProvider.get().performAction(EmbeddedNavigator.Action.Back)
            },
            defaultPaymentMethodId = savedPaymentMethodMutator.defaultPaymentMethodId,
        )
    }
}
