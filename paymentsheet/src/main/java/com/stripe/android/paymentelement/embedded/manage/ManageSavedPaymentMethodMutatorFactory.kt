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
import com.stripe.android.paymentsheet.repositories.SavedPaymentMethodRepository
import com.stripe.android.paymentsheet.ui.PaymentMethodRemovalDelayMillis
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal class ManageSavedPaymentMethodMutatorFactory @Inject constructor(
    private val eventReporter: EventReporter,
    private val savedPaymentMethodRepository: SavedPaymentMethodRepository,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    @IOContext private val workContext: CoroutineContext,
    @UIContext private val uiContext: CoroutineContext,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    private val updateScreenInteractorFactoryProvider: Provider<EmbeddedUpdateScreenInteractorFactory>,
) {
    private var createdMutator: SavedPaymentMethodMutator? = null

    fun createSavedPaymentMethodMutator(
        navigateBack: () -> Unit,
        close: (shouldInvokeRowSelectionCallback: Boolean) -> Unit,
        navigateToUpdate: (UpdatePaymentMethodInteractor) -> Unit,
    ): SavedPaymentMethodMutator {
        val mutator = SavedPaymentMethodMutator(
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
            eventReporter = eventReporter,
            coroutineScope = viewModelScope,
            workContext = workContext,
            uiContext = uiContext,
            savedPaymentMethodRepository = savedPaymentMethodRepository,
            selection = selectionHolder.selection,
            setSelection = selectionHolder::set,
            customerStateHolder = customerStateHolder,
            prePaymentMethodRemoveActions = {
                if (customerStateHolder.paymentMethods.value.size > 1) {
                    navigateBack()
                    withContext(workContext) {
                        delay(PaymentMethodRemovalDelayMillis)
                    }
                }
            },
            postPaymentMethodRemoveActions = {
                onPaymentMethodRemoved(close)
            },
            onUpdatePaymentMethod = { displayableSavedPaymentMethod, _, _, _, _ ->
                onUpdatePaymentMethod(displayableSavedPaymentMethod, navigateBack, navigateToUpdate)
            },
            isLinkEnabled = stateFlowOf(false),
            isNotPaymentFlow = false,
        )
        createdMutator = mutator
        return mutator
    }

    private fun onPaymentMethodRemoved(
        close: (shouldInvokeRowSelectionCallback: Boolean) -> Unit,
    ) {
        val shouldCloseSheet = customerStateHolder.paymentMethods.value.isEmpty()
        if (shouldCloseSheet) {
            close(false)
        }
    }

    private fun onUpdatePaymentMethod(
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
        navigateBack: () -> Unit,
        navigateToUpdate: (UpdatePaymentMethodInteractor) -> Unit,
    ) {
        if (displayableSavedPaymentMethod.savedPaymentMethod != SavedPaymentMethod.Unexpected) {
            val mutator = requireNotNull(createdMutator) {
                "SavedPaymentMethodMutator must be created before navigating to update"
            }
            navigateToUpdate(
                updateScreenInteractorFactoryProvider.get().createUpdateScreenInteractor(
                    displayableSavedPaymentMethod = displayableSavedPaymentMethod,
                    savedPaymentMethodMutator = mutator,
                    navigateBack = navigateBack,
                )
            )
        }
    }
}
