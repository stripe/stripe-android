package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher.Companion.HOSTED_SURFACE_PAYMENT_ELEMENT
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.FormHelper.FormType
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.utils.childScope
import com.stripe.android.paymentsheet.verticalmode.BankFormInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal fun interface EmbeddedPreferFormInteractorFactory {
    fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        configuration: EmbeddedPaymentElement.Configuration,
        walletsState: StateFlow<WalletsState?>,
        preferFormDisabled: Boolean,
    ): AddPaymentMethodInteractor?
}

internal class DefaultEmbeddedPreferFormInteractorFactory @Inject constructor(
    private val selectionHolder: EmbeddedSelectionHolder,
    private val formHelperFactory: EmbeddedFormHelperFactory,
    private val customerStateHolder: CustomerStateHolder,
    private val confirmationHandler: ConfirmationHandler,
    private val eventReporter: EventReporter,
    private val promotionsHelper: PaymentMethodMessagePromotionsHelper,
    private val validationStateHolder: EmbeddedContentValidationStateHolder,
    @ViewModelScope private val viewModelScope: CoroutineScope,
) : EmbeddedPreferFormInteractorFactory {
    @Suppress("LongMethod")
    override fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        configuration: EmbeddedPaymentElement.Configuration,
        walletsState: StateFlow<WalletsState?>,
        preferFormDisabled: Boolean,
    ): AddPaymentMethodInteractor? {
        if (!configuration.preferForm) return null

        val supportedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods()
        val selectedCode = selectionHolder.temporarySelection.value
            ?: (selectionHolder.selection.value as? PaymentSelection.New)
                ?.paymentMethodType
                ?.takeIf { preferFormDisabled }
            ?: supportedPaymentMethods.firstOrNull()
                ?.code
                ?.takeUnless { preferFormDisabled }
            ?: return null
        val scope = viewModelScope.childScope(Dispatchers.Main)
        val hasSavedPaymentMethods = customerStateHolder.paymentMethods.value.isNotEmpty()
        val formHelper = formHelperFactory.create(
            coroutineScope = scope,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = eventReporter,
            automaticallyLaunchedCardScanFormDataHelper =
                formHelperFactory.createAutomaticallyLaunchedCardScanFormDataHelper(
                    selectedPaymentMethodCode = selectedCode,
                    paymentMethodMetadata = paymentMethodMetadata,
                ),
            tapToAddHelper = null,
            selectionUpdater = selectionHolder::setSelection,
            setAsDefaultMatchesSaveForFutureUse = !hasSavedPaymentMethods,
            paymentMethodMessagePromotionsHelper = promotionsHelper,
            autocompleteAddressInteractorFactory = null,
        )
        if (!isPreferFormEligible(
                selectedFormType = formHelper.formTypeForCode(selectedCode),
            )
        ) {
            scope.cancel()
            return null
        }

        val bankFormInteractor = BankFormInteractor(
            updateSelection = selectionHolder::setSelection,
            paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
                paymentMethodMetadata.paymentMethodIncentive
            ),
        )
        return DefaultAddPaymentMethodInteractor(
            initiallySelectedPaymentMethodType = selectedCode,
            selection = selectionHolder.selection,
            processing = confirmationHandler.state.mapAsStateFlow {
                it is ConfirmationHandler.State.Confirming
            },
            validationRequested = validationStateHolder.validationRequested,
            incentive = bankFormInteractor.paymentMethodIncentiveInteractor.displayedIncentive,
            supportedPaymentMethods = supportedPaymentMethods,
            createFormArguments = formHelper::createFormArguments,
            formElementsForCode = formHelper::formElementsForCode,
            clearErrorMessages = {},
            reportFieldInteraction = eventReporter::onPaymentMethodFormInteraction,
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            reportPaymentMethodTypeSelected = eventReporter::onSelectPaymentMethod,
            reportPromotionDisplayed = { code ->
                promotionsHelper.reportPromotionDisplayed(code, paymentMethodMetadata)
            },
            createUSBankAccountFormArguments = { code ->
                createUsBankAccountFormArguments(
                    code = code,
                    hasSavedPaymentMethods = hasSavedPaymentMethods,
                    paymentMethodMetadata = paymentMethodMetadata,
                    bankFormInteractor = bankFormInteractor,
                )
            },
            coroutineScope = scope,
            uiContext = Dispatchers.Main,
            onInitiallyDisplayedPaymentMethodVisibilitySnapshot = { _, _ -> },
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
        )
    }

    private fun createUsBankAccountFormArguments(
        code: PaymentMethodCode,
        hasSavedPaymentMethods: Boolean,
        paymentMethodMetadata: PaymentMethodMetadata,
        bankFormInteractor: BankFormInteractor,
    ): USBankAccountFormArguments {
        return USBankAccountFormArguments.createForEmbedded(
            paymentMethodMetadata = paymentMethodMetadata,
            selectedPaymentMethodCode = code,
            hostedSurface = HOSTED_SURFACE_PAYMENT_ELEMENT,
            isCompleteFlow = false,
            draftPaymentSelection = null,
            bankFormInteractor = bankFormInteractor,
            hasSavedPaymentMethods = hasSavedPaymentMethods,
            autocompleteAddressInteractorFactory = null,
            onAnalyticsEvent = eventReporter::onUsBankAccountFormEvent,
            onMandateTextChanged = { _, _ -> },
            onUpdatePrimaryButtonUIState = {},
            onError = {},
            onFormCompleted = { eventReporter.onPaymentMethodFormCompleted(PaymentMethod.Type.USBankAccount.code) },
        )
    }
}

internal fun isPreferFormEligible(
    selectedFormType: FormType,
): Boolean {
    return selectedFormType == FormType.UserInteractionRequired
}
