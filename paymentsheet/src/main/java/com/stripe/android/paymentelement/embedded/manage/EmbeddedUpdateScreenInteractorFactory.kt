package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import javax.inject.Inject
import javax.inject.Provider

internal fun interface EmbeddedUpdateScreenInteractorFactory {
    fun createUpdateScreenInteractor(
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod
    ): UpdatePaymentMethodInteractor
}

internal class DefaultEmbeddedUpdateScreenInteractorFactory @Inject constructor(
    private val savedPaymentMethodMutatorProvider: Provider<SavedPaymentMethodMutator>,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val eventReporter: EventReporter,
    private val embeddedNavigatorProvider: Provider<EmbeddedNavigator>,
) : EmbeddedUpdateScreenInteractorFactory {
    override fun createUpdateScreenInteractor(
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod
    ): UpdatePaymentMethodInteractor {
        return DefaultUpdatePaymentMethodInteractor(
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            canRemove = customerStateHolder.canRemove.value,
            canUpdateFullPaymentMethodDetails = customerStateHolder.canUpdateFullPaymentMethodDetails.value,
            displayableSavedPaymentMethod = displayableSavedPaymentMethod,
            cardBrandFilter = paymentMethodMetadata.cardBrandFilter,
            addressCollectionMode = paymentMethodMetadata.billingDetailsCollectionConfiguration.address,
            allowedBillingCountries =
                paymentMethodMetadata.billingDetailsCollectionConfiguration.allowedBillingCountries,
            removeExecutor = createRemoveExecutor(),
            updatePaymentMethodExecutor = createUpdatePaymentMethodExecutor(),
            setDefaultPaymentMethodExecutor = { method ->
                savedPaymentMethodMutatorProvider.get().setDefaultPaymentMethod(method)
            },
            onBrandChoiceSelected = {
                eventReporter.onBrandChoiceSelected(
                    source = EventReporter.CardBrandChoiceEventSource.Edit,
                    selectedBrand = it
                )
            },
            shouldShowSetAsDefaultCheckbox = (
                paymentMethodMetadata.customerMetadata?.isPaymentMethodSetAsDefaultEnabled == true
                ),
            isDefaultPaymentMethod = (
                displayableSavedPaymentMethod.isDefaultPaymentMethod(
                    defaultPaymentMethodId = customerStateHolder.customer.value?.defaultPaymentMethodId
                )
                ),
            removeMessage = paymentMethodMetadata.customerMetadata?.removePaymentMethod
                ?.removeMessage(paymentMethodMetadata.merchantName),
            onUpdateSuccess = {
                embeddedNavigatorProvider.get().performAction(EmbeddedNavigator.Action.Back)
            },
        )
    }

    private fun createRemoveExecutor(): suspend (com.stripe.android.model.PaymentMethod) -> Throwable? {
        return { method ->
            val result = savedPaymentMethodMutatorProvider.get().removePaymentMethodInEditScreen(method)
            if (result == null && method.id == currentSavedSelection()?.paymentMethod?.id) {
                selectionHolder.set(null)
            }
            result
        }
    }

    private fun createUpdatePaymentMethodExecutor():
        suspend (com.stripe.android.model.PaymentMethod, CardUpdateParams) ->
        Result<com.stripe.android.model.PaymentMethod> {
        return { method, cardUpdateParams ->
            savedPaymentMethodMutatorProvider.get().modifyCardPaymentMethod(
                paymentMethod = method,
                cardUpdateParams = cardUpdateParams,
                onSuccess = { updatedPaymentMethod ->
                    currentSavedSelection()?.takeIf { currentSelection ->
                        updatedPaymentMethod.id == currentSelection.paymentMethod.id
                    }?.let { currentSelection ->
                        selectionHolder.set(
                            PaymentSelection.Saved(
                                paymentMethod = updatedPaymentMethod,
                                linkBrand = currentSelection.linkBrand,
                            )
                        )
                    }
                },
            )
        }
    }

    private fun currentSavedSelection(): PaymentSelection.Saved? {
        return selectionHolder.selection.value as? PaymentSelection.Saved
    }
}
