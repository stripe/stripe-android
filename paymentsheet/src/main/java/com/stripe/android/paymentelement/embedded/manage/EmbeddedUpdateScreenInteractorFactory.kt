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
    private val manageNavigatorProvider: Provider<ManageNavigator>,
) : EmbeddedUpdateScreenInteractorFactory {
    override fun createUpdateScreenInteractor(
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod
    ): UpdatePaymentMethodInteractor {
        return DefaultUpdatePaymentMethodInteractor(
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            canRemove = customerStateHolder.canRemove.value,
            displayableSavedPaymentMethod,
            cardBrandFilter = paymentMethodMetadata.cardBrandFilter,
            addressCollectionMode = paymentMethodMetadata.billingDetailsCollectionConfiguration.address,
            removeExecutor = { method ->
                val result = savedPaymentMethodMutatorProvider.get().removePaymentMethodInEditScreen(method)
                if (result == null) {
                    val currentSelection = selectionHolder.selection.value
                    if (method.id == (currentSelection as? PaymentSelection.Saved)?.paymentMethod?.id) {
                        selectionHolder.set(null)
                    }
                }
                result
            },
            updateCardBrandExecutor = { method, brand ->
                savedPaymentMethodMutatorProvider.get().modifyCardPaymentMethod(
                    paymentMethod = method,
                    brand = brand,
                    onSuccess = { paymentMethod ->
                        val currentSelection = selectionHolder.selection.value
                        if (paymentMethod.id == (currentSelection as? PaymentSelection.Saved)?.paymentMethod?.id) {
                            selectionHolder.set(PaymentSelection.Saved(paymentMethod))
                        }
                    },
                )
            },
            setDefaultPaymentMethodExecutor = { method ->
                savedPaymentMethodMutatorProvider.get().setDefaultPaymentMethod(method)
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
            shouldShowSetAsDefaultCheckbox = (
                paymentMethodMetadata.customerMetadata?.isPaymentMethodSetAsDefaultEnabled == true &&
                    !displayableSavedPaymentMethod.isDefaultPaymentMethod(
                        defaultPaymentMethodId = customerStateHolder.customer.value?.defaultPaymentMethodId
                    )
                ),
            onUpdateSuccess = {
                manageNavigatorProvider.get().performAction(ManageNavigator.Action.Back)
            },
        )
    }
}
