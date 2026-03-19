package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import javax.inject.Inject

internal fun interface EmbeddedUpdateScreenInteractorFactory {
    fun createUpdateScreenInteractor(
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
        savedPaymentMethodMutator: SavedPaymentMethodMutator,
        navigateBack: () -> Unit,
    ): UpdatePaymentMethodInteractor
}

internal class DefaultEmbeddedUpdateScreenInteractorFactory @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val eventReporter: EventReporter,
) : EmbeddedUpdateScreenInteractorFactory {
    override fun createUpdateScreenInteractor(
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
        savedPaymentMethodMutator: SavedPaymentMethodMutator,
        navigateBack: () -> Unit,
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
            removeExecutor = { method ->
                val result = savedPaymentMethodMutator.removePaymentMethodInEditScreen(method)
                if (result == null) {
                    val currentSelection = selectionHolder.selection.value
                    if (method.id == (currentSelection as? PaymentSelection.Saved)?.paymentMethod?.id) {
                        selectionHolder.set(null)
                    }
                }
                result
            },
            updatePaymentMethodExecutor = { method, cardUpdateParams ->
                savedPaymentMethodMutator.modifyCardPaymentMethod(
                    paymentMethod = method,
                    cardUpdateParams = cardUpdateParams,
                    onSuccess = { paymentMethod ->
                        val currentSelection = selectionHolder.selection.value
                        if (paymentMethod.id == (currentSelection as? PaymentSelection.Saved)?.paymentMethod?.id) {
                            selectionHolder.set(PaymentSelection.Saved(paymentMethod))
                        }
                    },
                )
            },
            setDefaultPaymentMethodExecutor = { method ->
                savedPaymentMethodMutator.setDefaultPaymentMethod(method)
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
            onUpdateSuccess = navigateBack,
        )
    }
}
