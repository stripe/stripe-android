package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.SavedPaymentMethod
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.ui.PaymentMethodRemovalDelayMillis
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    @UIContext private val uiContext: CoroutineContext,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    private val updateScreenInteractorFactoryProvider: Provider<EmbeddedUpdateScreenInteractorFactory>,
) {
    fun createSavedPaymentMethodMutator(): SavedPaymentMethodMutator {
        return SavedPaymentMethodMutator(
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
            eventReporter = eventReporter,
            coroutineScope = viewModelScope,
            workContext = workContext,
            uiContext = uiContext,
            customerRepository = customerRepository,
            selection = selectionHolder.selection,
            setSelection = {
                viewModelScope.launch {
                    selectionHolder::set
                }
            },
            customerStateHolder = customerStateHolder,
            prePaymentMethodRemoveActions = {
                if (customerStateHolder.paymentMethods.value.size > 1) {
                    manageNavigatorProvider.get().performAction(ManageNavigator.Action.Back)
                    withContext(workContext) {
                        delay(PaymentMethodRemovalDelayMillis)
                    }
                }
            },
            postPaymentMethodRemoveActions = ::onPaymentMethodRemoved,
            onUpdatePaymentMethod = { displayableSavedPaymentMethod, _, _, _, _ ->
                onUpdatePaymentMethod(displayableSavedPaymentMethod)
            },
            isLinkEnabled = stateFlowOf(false), // Link is never enabled in the manage screen.
            isNotPaymentFlow = false,
        )
    }

    private fun onPaymentMethodRemoved() {
        val shouldCloseSheet = customerStateHolder.paymentMethods.value.isEmpty()
        if (shouldCloseSheet) {
            selectionHolder.set(null)
            manageNavigatorProvider.get().performAction(ManageNavigator.Action.Close)
        }
    }

    private fun onUpdatePaymentMethod(
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    ) {
        if (displayableSavedPaymentMethod.savedPaymentMethod != SavedPaymentMethod.Unexpected) {
            manageNavigatorProvider.get().performAction(
                ManageNavigator.Action.GoToScreen(
                    screen = ManageNavigator.Screen.Update(
                        interactor = updateScreenInteractorFactoryProvider.get().createUpdateScreenInteractor(
                            displayableSavedPaymentMethod
                        )
                    )
                )
            )
        }
    }
}
