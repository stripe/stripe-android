package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
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
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Subcomponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Scope
import javax.inject.Singleton

internal class FormActivityViewModel @Inject constructor(
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
) : ViewModel() {

    fun initializeFormInteractor(
        paymentMethodMetadata: PaymentMethodMetadata,
        selectedPaymentMethodCode: PaymentMethodCode,
    ) {
        val formHelper = createFormHelper(
            coroutineScope = viewModelScope,
            paymentMethodMetadata = paymentMethodMetadata
        )

        val paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
            paymentMethodMetadata.paymentMethodIncentive
        )
        val formInteractor = DefaultVerticalModeFormInteractor(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            formArguments = formHelper.createFormArguments(selectedPaymentMethodCode),
            formElements = formHelper.formElementsForCode(selectedPaymentMethodCode),
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            usBankAccountArguments = createUsBankAccountFormArguments(
                paymentMethodMetadata, selectedPaymentMethodCode
            ),
            reportFieldInteraction = {
            },
            headerInformation = null,
            canGoBackDelegate = { false },
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            processing = stateFlowOf(false),
            paymentMethodIncentive = paymentMethodIncentiveInteractor.displayedIncentive,
            coroutineScope = viewModelScope,
        )
    }

    private fun createFormHelper(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): FormHelper {
        val linkInlineHandler = createLinkInlineHandler(coroutineScope)
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
            selectionUpdater = {
            },
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            onLinkInlineSignupStateChanged = linkInlineHandler::onStateUpdated,
        )
    }

    private fun createLinkInlineHandler(
        coroutineScope: CoroutineScope,
    ): LinkInlineHandler {
        return LinkInlineHandler(
            coroutineScope = coroutineScope,
            payWithLink = { _, _, _ ->
            },
            selection = selectionHolder.selection,
            updateLinkPrimaryButtonUiState = {
            },
            primaryButtonLabel = stateFlowOf(null),
            shouldCompleteLinkFlowInline = false,
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
            showCheckbox = isSaveForFutureUseValueChangeable &&
                // Instant Debits does not support saving for future use
                instantDebits.not(),
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
}