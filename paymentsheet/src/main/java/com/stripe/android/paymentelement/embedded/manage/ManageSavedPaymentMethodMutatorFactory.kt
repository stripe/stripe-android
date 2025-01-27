package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.SavedPaymentMethod
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal class ManageSavedPaymentMethodMutatorFactory @Inject constructor(
    private val eventReporter: EventReporter,
    private val customerRepository: CustomerRepository,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val manageNavigatorProvider: Provider<ManageNavigator>,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    @IOContext private val workContext: CoroutineContext,
    @ViewModelScope private val viewModelScope: CoroutineScope,
) {
    fun createSavedPaymentMethodMutator(): SavedPaymentMethodMutator {
        return SavedPaymentMethodMutator(
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
            eventReporter = eventReporter,
            coroutineScope = viewModelScope,
            workContext = workContext,
            customerRepository = customerRepository,
            selection = selectionHolder.selection,
            clearSelection = {
                selectionHolder.set(null)
            },
            customerStateHolder = customerStateHolder,
            onPaymentMethodRemoved = ::onPaymentMethodRemoved,
            onUpdatePaymentMethod = ::onUpdatePaymentMethod,
            navigationPop = {
                manageNavigatorProvider.get().performAction(ManageNavigator.Action.Back)
            },
            isLinkEnabled = stateFlowOf(false), // Link is never enabled in the manage screen.
            isNotPaymentFlow = false,
        )
    }

    private fun onPaymentMethodRemoved() {
        // This should mostly be easy, just always pop?
        // TODO()
//        val currentScreen = manageNavigator.screen.value
//        val shouldResetToAddPaymentMethodForm =
//            customerStateHolder.paymentMethods.value.isEmpty() &&
//                currentScreen is PaymentSheetScreen.SelectSavedPaymentMethods
//
//        if (shouldResetToAddPaymentMethodForm) {
//            val interactor = DefaultAddPaymentMethodInteractor.create(
//                viewModel = viewModel,
//                paymentMethodMetadata = requireNotNull(viewModel.paymentMethodMetadata.value),
//            )
//            val screen = PaymentSheetScreen.AddFirstPaymentMethod(interactor)
//            viewModel.navigationHandler.resetTo(listOf(screen))
//        }
    }

    private fun onUpdatePaymentMethod(
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
        canRemove: Boolean,
        performRemove: suspend () -> Throwable?,
        updateExecutor: suspend (brand: CardBrand) -> Result<PaymentMethod>,
    ) {
        if (displayableSavedPaymentMethod.savedPaymentMethod != SavedPaymentMethod.Unexpected) {
            val interactor = DefaultUpdatePaymentMethodInteractor(
                isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
                canRemove = canRemove,
                displayableSavedPaymentMethod,
                cardBrandFilter = paymentMethodMetadata.cardBrandFilter,
                removeExecutor = { method ->
                    performRemove()
                },
                updateExecutor = { method, brand ->
                    updateExecutor(brand)
                },
                onBrandChoiceOptionsShown = {
                    eventReporter.onShowPaymentOptionBrands(
                        source = EventReporter.CardBrandChoiceEventSource.Edit,
                        selectedBrand = it
                    )
                },
                onBrandChoiceOptionsDismissed = {
                    eventReporter.onHidePaymentOptionBrands(
                        source = EventReporter.CardBrandChoiceEventSource.Edit,
                        selectedBrand = it
                    )
                },
            )
            manageNavigatorProvider.get().performAction(
                ManageNavigator.Action.GoToScreen(ManageNavigator.Screen.Update(interactor))
            )
        }
    }
}
