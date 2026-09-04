package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityStateHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.DefaultManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.uicore.utils.mapAsStateFlow
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
    private val linkAccountHolder: LinkAccountHolder,
    private val eventReporter: EventReporter,
    private val embeddedNavigatorProvider: Provider<EmbeddedNavigator>,
    private val launchMode: EmbeddedLaunchMode,
    private val sheetActivityStateHolder: SheetActivityStateHolder,
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
                eventReporter.onSelectPaymentOption(savedPmSelection)
                sheetActivityStateHolder.selectSavedPaymentMethod(savedPmSelection)
            },
            onUpdatePaymentMethod = savedPaymentMethodMutator::updatePaymentMethod,
            navigateBack = {
                val action = when (launchMode) {
                    EmbeddedLaunchMode.PaymentOptions -> EmbeddedNavigator.Action.Back
                    EmbeddedLaunchMode.Manage,
                    is EmbeddedLaunchMode.Form -> EmbeddedNavigator.Action.Close(true)
                }
                embeddedNavigatorProvider.get().performAction(action)
            },
            defaultPaymentMethodId = savedPaymentMethodMutator.defaultPaymentMethodId,
            linkAccount = linkAccountHolder.linkAccountInfo,
            processing = sheetActivityStateHolder.state.mapAsStateFlow { it.isProcessing },
            pendingPaymentMethodId = sheetActivityStateHolder.state.mapAsStateFlow {
                it.pendingPaymentMethodId
            },
            error = sheetActivityStateHolder.state.mapAsStateFlow { it.error },
            navigateBackAfterSelection = false,
        )
    }
}
