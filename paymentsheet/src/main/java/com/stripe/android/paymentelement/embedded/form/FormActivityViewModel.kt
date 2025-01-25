package com.stripe.android.paymentelement.embedded.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher.Companion.HOSTED_SURFACE_PAYMENT_ELEMENT
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.NewOrExternalPaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.verticalmode.BankFormInteractor
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import javax.inject.Inject

internal class FormActivityViewModel @Inject constructor(
    paymentMethodMetadata: PaymentMethodMetadata,
    selectedPaymentMethodCode: PaymentMethodCode,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
) : ViewModel() {

    val formInteractor = initializeFormInteractor(paymentMethodMetadata, selectedPaymentMethodCode)

    private fun initializeFormInteractor(
        paymentMethodMetadata: PaymentMethodMetadata,
        selectedPaymentMethodCode: PaymentMethodCode,
    ): DefaultVerticalModeFormInteractor {
        val formHelper = createFormHelper(paymentMethodMetadata = paymentMethodMetadata)

        return DefaultVerticalModeFormInteractor(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            formArguments = formHelper.createFormArguments(selectedPaymentMethodCode),
            formElements = formHelper.formElementsForCode(selectedPaymentMethodCode),
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            usBankAccountArguments = createUsBankAccountFormArguments(
                paymentMethodMetadata,
                selectedPaymentMethodCode
            ),
            reportFieldInteraction = {
            },
            headerInformation = null,
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            processing = stateFlowOf(false),
            paymentMethodIncentive = PaymentMethodIncentiveInteractor(
                paymentMethodMetadata.paymentMethodIncentive
            ).displayedIncentive,
            coroutineScope = viewModelScope,
        )
    }

    private fun createFormHelper(paymentMethodMetadata: PaymentMethodMetadata): FormHelper {
        return DefaultFormHelper(
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            paymentMethodMetadata = paymentMethodMetadata,
            newPaymentSelectionProvider = {
                when (val currentSelection = selectionHolder.selection.value) {
                    is PaymentSelection.ExternalPaymentMethod -> {
                        NewOrExternalPaymentSelection.External(currentSelection)
                    }
                    is PaymentSelection.New -> {
                        NewOrExternalPaymentSelection.New(currentSelection)
                    }
                    else -> null
                }
            },
            selectionUpdater = { selection ->
                selectionHolder.set(selection)
            },
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            linkInlineHandler = LinkInlineHandler.create(),
            coroutineScope = viewModelScope
        )
    }

    private fun createUsBankAccountFormArguments(
        paymentMethodMetadata: PaymentMethodMetadata,
        selectedPaymentMethodCode: PaymentMethodCode,
    ): USBankAccountFormArguments {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = selectedPaymentMethodCode,
            intent = paymentMethodMetadata.stripeIntent,
            paymentMethodSaveConsentBehavior = paymentMethodMetadata.paymentMethodSaveConsentBehavior,
            hasCustomerConfiguration = paymentMethodMetadata.hasCustomerConfiguration,
        )
        val instantDebits = selectedPaymentMethodCode == PaymentMethod.Type.Link.code
        val bankFormInteractor = BankFormInteractor(
            updateSelection = { selectionHolder.set(it) },
            paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
                paymentMethodMetadata.paymentMethodIncentive
            )
        )
        return USBankAccountFormArguments(
            showCheckbox = isSaveForFutureUseValueChangeable && instantDebits.not(),
            hostedSurface = HOSTED_SURFACE_PAYMENT_ELEMENT,
            instantDebits = instantDebits,
            linkMode = paymentMethodMetadata.linkMode,
            onBehalfOf = null,
            isCompleteFlow = false,
            isPaymentFlow = paymentMethodMetadata.stripeIntent is PaymentIntent,
            stripeIntentId = paymentMethodMetadata.stripeIntent.id,
            clientSecret = paymentMethodMetadata.stripeIntent.clientSecret,
            shippingDetails = paymentMethodMetadata.shippingDetails,
            draftPaymentSelection = null,
            onMandateTextChanged = { _, _ ->
            },
            onLinkedBankAccountChanged = bankFormInteractor::handleLinkedBankAccountChanged,
            onUpdatePrimaryButtonUIState = {
            },
            onUpdatePrimaryButtonState = {
            },
            onError = {
            },
            incentive = paymentMethodMetadata.paymentMethodIncentive,
        )
    }

    class Factory(
        private val argSupplier: () -> FormContract.Args
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = argSupplier()
            val component = DaggerFormActivityComponent.builder()
                .paymentMethodMetadata(args.paymentMethodMetadata)
                .selectedPaymentMethodCode(args.selectedPaymentMethodCode)
                .context(extras.requireApplication())
                .savedStateHandle(extras.createSavedStateHandle())
                .build()

            return component.viewModel as T
        }
    }
}
