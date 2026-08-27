package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.common.nfcscan.IsNfcScanningAvailable
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.utils.childScope
import com.stripe.android.paymentsheet.verticalmode.BankFormInteractor
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor
import com.stripe.android.ui.core.elements.AutomaticallyLaunchedCardScanFormDataHelper
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal class PaymentMethodFormFactory @Inject constructor(
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val savedStateHandle: SavedStateHandle,
    private val isNfcScanningAvailable: IsNfcScanningAvailable,
) {
    data class Dependencies(
        val coroutineScope: CoroutineScope,
        val selection: StateFlow<PaymentSelection?>,
        val processing: StateFlow<Boolean>,
        val validationRequested: SharedFlow<Unit>,
        val newPaymentSelectionProvider: (PaymentMethodCode) -> NewPaymentOptionSelection?,
        val selectionUpdater: (PaymentSelection?) -> Unit,
        val clearErrorMessages: () -> Unit,
        val reportFieldInteraction: (PaymentMethodCode) -> Unit,
        val eventReporter: EventReporter,
        val tapToAddHelper: TapToAddHelper?,
        val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
        val setAsDefaultMatchesSaveForFutureUse: Boolean,
        val isCompleteFlow: Boolean,
        val shippingDetails: AddressDetails?,
        val draftPaymentSelectionProvider: () -> PaymentSelection?,
        val onMandateTextChanged: (ResolvableString?, Boolean) -> Unit,
        val onUpdatePrimaryButtonUIState: ((PrimaryButton.UIState?) -> PrimaryButton.UIState?) -> Unit,
        val onUpdatePrimaryButtonState: (PrimaryButton.State) -> Unit,
        val onError: (ResolvableString?) -> Unit,
        val termsDisplayProvider: (PaymentMethodCode) -> PaymentSheet.TermsDisplay,
        val hasAutomaticallyLaunchedCardScanProvider: (PaymentMethodCode) -> Boolean,
    )

    data class FormHelperArguments(
        val coroutineScope: CoroutineScope,
        val linkInlineHandler: LinkInlineHandler,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val newPaymentSelectionProvider: (PaymentMethodCode) -> NewPaymentOptionSelection?,
        val selectionUpdater: (PaymentSelection?) -> Unit,
        val eventReporter: EventReporter,
        val setAsDefaultMatchesSaveForFutureUse: Boolean,
        val automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
        val tapToAddHelper: TapToAddHelper?,
        val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
        val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
    )

    data class FormDefinitionArguments(
        val coroutineScope: CoroutineScope,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val newPaymentSelectionProvider: (PaymentMethodCode) -> NewPaymentOptionSelection?,
        val setAsDefaultMatchesSaveForFutureUse: Boolean,
        val automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
        val tapToAddHelper: TapToAddHelper?,
        val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
        val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
    )

    fun createAddPaymentMethodInteractor(
        initiallySelectedPaymentMethodType: PaymentMethodCode,
        paymentMethodMetadata: PaymentMethodMetadata,
        dependencies: Dependencies,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
        onInitiallyDisplayedPaymentMethodVisibilitySnapshot: (List<String>, List<String>) -> Unit,
    ): AddPaymentMethodInteractor {
        val coroutineScope = dependencies.coroutineScope.childScope(Dispatchers.Main)
        val bankFormInteractor = createBankFormInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            selectionUpdater = dependencies.selectionUpdater,
        )
        val formHelper = createFormHelper(
            formHelperArguments(
                coroutineScope = coroutineScope,
                linkInlineHandler = LinkInlineHandler.create(),
                paymentMethodMetadata = paymentMethodMetadata,
                dependencies = dependencies,
                paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
                selectedPaymentMethodCode = initiallySelectedPaymentMethodType,
                createAutomaticallyLaunchedCardScanFormDataHelper = true,
            )
        )

        return DefaultAddPaymentMethodInteractor(
            initiallySelectedPaymentMethodType = initiallySelectedPaymentMethodType,
            selection = dependencies.selection,
            processing = dependencies.processing,
            incentive = bankFormInteractor.paymentMethodIncentiveInteractor.displayedIncentive,
            supportedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods(),
            createFormArguments = formHelper::createFormArguments,
            formElementsForCode = formHelper::formElementsForCode,
            clearErrorMessages = dependencies.clearErrorMessages,
            reportFieldInteraction = dependencies.reportFieldInteraction,
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            reportPaymentMethodTypeSelected = dependencies.eventReporter::onSelectPaymentMethod,
            reportPromotionDisplayed = { code ->
                paymentMethodMessagePromotionsHelper?.reportPromotionDisplayed(code, paymentMethodMetadata)
            },
            createUSBankAccountFormArguments = { code ->
                createUSBankAccountFormArguments(
                    selectedPaymentMethodCode = code,
                    paymentMethodMetadata = paymentMethodMetadata,
                    dependencies = dependencies,
                    bankFormInteractor = bankFormInteractor,
                )
            },
            coroutineScope = coroutineScope,
            validationRequested = dependencies.validationRequested,
            uiContext = Dispatchers.Main,
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            onInitiallyDisplayedPaymentMethodVisibilitySnapshot =
                onInitiallyDisplayedPaymentMethodVisibilitySnapshot,
        )
    }

    fun createVerticalModeFormInteractor(
        selectedPaymentMethodCode: PaymentMethodCode,
        paymentMethodMetadata: PaymentMethodMetadata,
        customerHasSavedPaymentMethods: Boolean,
        dependencies: Dependencies,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
        bankFormInteractor: BankFormInteractor,
        onMandateOnlyFormReady: ((FormArguments) -> Unit)?,
    ): VerticalModeFormInteractor {
        val coroutineScope = dependencies.coroutineScope.childScope(Dispatchers.Default)
        val formHelperScope = coroutineScope.childScope(Dispatchers.Main)
        val formHelper = createFormHelper(
            formHelperArguments(
                coroutineScope = formHelperScope,
                linkInlineHandler = LinkInlineHandler.create(),
                paymentMethodMetadata = paymentMethodMetadata,
                dependencies = dependencies,
                paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
                selectedPaymentMethodCode = selectedPaymentMethodCode,
                createAutomaticallyLaunchedCardScanFormDataHelper = true,
            )
        )
        val formArguments = formHelper.createFormArguments(selectedPaymentMethodCode)
        if (formHelper.formTypeForCode(selectedPaymentMethodCode) is FormHelper.FormType.MandateOnly) {
            onMandateOnlyFormReady?.invoke(formArguments)
        }

        return DefaultVerticalModeFormInteractor(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            formArguments = formArguments,
            formElements = formHelper.formElementsForCode(selectedPaymentMethodCode),
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            usBankAccountArguments = createUSBankAccountFormArguments(
                selectedPaymentMethodCode = selectedPaymentMethodCode,
                paymentMethodMetadata = paymentMethodMetadata,
                dependencies = dependencies,
                bankFormInteractor = bankFormInteractor,
            ),
            reportFieldInteraction = dependencies.reportFieldInteraction,
            headerInformation = paymentMethodMetadata.formHeaderInformationForCode(
                code = selectedPaymentMethodCode,
                customerHasSavedPaymentMethods = customerHasSavedPaymentMethods,
            ),
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            processing = dependencies.processing,
            paymentMethodIncentive = bankFormInteractor.paymentMethodIncentiveInteractor.displayedIncentive,
            validationRequested = dependencies.validationRequested,
            coroutineScope = coroutineScope,
            uiContext = Dispatchers.Main,
        )
    }

    fun createBankFormInteractor(
        paymentMethodMetadata: PaymentMethodMetadata,
        selectionUpdater: (PaymentSelection?) -> Unit,
    ): BankFormInteractor {
        return BankFormInteractor(
            updateSelection = selectionUpdater,
            paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
                incentive = paymentMethodMetadata.paymentMethodIncentive,
            ),
        )
    }

    fun createFormHelper(
        coroutineScope: CoroutineScope,
        linkInlineHandler: LinkInlineHandler,
        paymentMethodMetadata: PaymentMethodMetadata,
        dependencies: Dependencies,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
        selectedPaymentMethodCode: PaymentMethodCode,
        createAutomaticallyLaunchedCardScanFormDataHelper: Boolean,
    ): FormHelper {
        return createFormHelper(
            formHelperArguments(
                coroutineScope = coroutineScope,
                linkInlineHandler = linkInlineHandler,
                paymentMethodMetadata = paymentMethodMetadata,
                dependencies = dependencies,
                paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
                selectedPaymentMethodCode = selectedPaymentMethodCode,
                createAutomaticallyLaunchedCardScanFormDataHelper =
                    createAutomaticallyLaunchedCardScanFormDataHelper,
            )
        )
    }

    fun createFormHelper(arguments: FormHelperArguments): FormHelper {
        return DefaultFormHelper(
            coroutineScope = arguments.coroutineScope,
            linkInlineHandler = arguments.linkInlineHandler,
            paymentMethodMetadata = arguments.paymentMethodMetadata,
            selectionUpdater = arguments.selectionUpdater,
            eventReporter = arguments.eventReporter,
            savedStateHandle = savedStateHandle,
            formDefinitionFactory = createFormDefinitionFactory(
                arguments = FormDefinitionArguments(
                    coroutineScope = arguments.coroutineScope,
                    paymentMethodMetadata = arguments.paymentMethodMetadata,
                    newPaymentSelectionProvider = arguments.newPaymentSelectionProvider,
                    setAsDefaultMatchesSaveForFutureUse = arguments.setAsDefaultMatchesSaveForFutureUse,
                    automaticallyLaunchedCardScanFormDataHelper =
                        arguments.automaticallyLaunchedCardScanFormDataHelper,
                    tapToAddHelper = arguments.tapToAddHelper,
                    paymentMethodMessagePromotionsHelper = arguments.paymentMethodMessagePromotionsHelper,
                    autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
                ),
                linkInlineHandler = arguments.linkInlineHandler,
            ),
        )
    }

    fun createFormDefinitionFactory(
        arguments: FormDefinitionArguments,
        linkInlineHandler: LinkInlineHandler,
    ): FormDefinitionFactory {
        return DefaultFormDefinitionFactory(
            coroutineScope = arguments.coroutineScope,
            linkInlineHandler = linkInlineHandler,
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            paymentMethodMetadata = arguments.paymentMethodMetadata,
            newPaymentSelectionProvider = arguments.newPaymentSelectionProvider,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            setAsDefaultMatchesSaveForFutureUse = arguments.setAsDefaultMatchesSaveForFutureUse,
            autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
            isLinkUI = false,
            automaticallyLaunchedCardScanFormDataHelper =
                arguments.automaticallyLaunchedCardScanFormDataHelper,
            tapToAddHelper = arguments.tapToAddHelper,
            paymentMethodMessagePromotionsHelper = arguments.paymentMethodMessagePromotionsHelper,
            isNfcScanningAvailable = isNfcScanningAvailable,
        )
    }

    private fun formHelperArguments(
        coroutineScope: CoroutineScope,
        linkInlineHandler: LinkInlineHandler,
        paymentMethodMetadata: PaymentMethodMetadata,
        dependencies: Dependencies,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
        selectedPaymentMethodCode: PaymentMethodCode,
        createAutomaticallyLaunchedCardScanFormDataHelper: Boolean,
    ): FormHelperArguments {
        return FormHelperArguments(
            coroutineScope = coroutineScope,
            linkInlineHandler = linkInlineHandler,
            paymentMethodMetadata = paymentMethodMetadata,
            newPaymentSelectionProvider = dependencies.newPaymentSelectionProvider,
            selectionUpdater = dependencies.selectionUpdater,
            eventReporter = dependencies.eventReporter,
            setAsDefaultMatchesSaveForFutureUse = dependencies.setAsDefaultMatchesSaveForFutureUse,
            automaticallyLaunchedCardScanFormDataHelper = if (createAutomaticallyLaunchedCardScanFormDataHelper) {
                AutomaticallyLaunchedCardScanFormDataHelper(
                    openCardScanAutomaticallyConfig = paymentMethodMetadata.openCardScanAutomatically,
                    savedStateHandle = savedStateHandle,
                    hasAutomaticallyLaunchedCardScanInitialValue =
                        dependencies.hasAutomaticallyLaunchedCardScanProvider(selectedPaymentMethodCode),
                )
            } else {
                null
            },
            tapToAddHelper = dependencies.tapToAddHelper,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
            autocompleteAddressInteractorFactory = dependencies.autocompleteAddressInteractorFactory,
        )
    }

    private fun createUSBankAccountFormArguments(
        selectedPaymentMethodCode: PaymentMethodCode,
        paymentMethodMetadata: PaymentMethodMetadata,
        dependencies: Dependencies,
        bankFormInteractor: BankFormInteractor,
    ): USBankAccountFormArguments {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = selectedPaymentMethodCode,
            intent = paymentMethodMetadata.stripeIntent,
            paymentMethodSaveConsentBehavior = paymentMethodMetadata.customerMetadata?.saveConsent,
            hasCustomerConfiguration = paymentMethodMetadata.customerMetadata != null,
        )
        val instantDebits = selectedPaymentMethodCode == PaymentMethod.Type.Link.code
        val stripeIntent = paymentMethodMetadata.stripeIntent

        return USBankAccountFormArguments(
            showCheckbox = isSaveForFutureUseValueChangeable && instantDebits.not(),
            hostedSurface = CollectBankAccountLauncher.HOSTED_SURFACE_PAYMENT_ELEMENT,
            instantDebits = instantDebits,
            linkMode = paymentMethodMetadata.linkMode,
            onBehalfOf = paymentMethodMetadata.onBehalfOf,
            isCompleteFlow = dependencies.isCompleteFlow,
            isPaymentFlow = stripeIntent is PaymentIntent,
            stripeIntentId = stripeIntent.id,
            clientSecret = stripeIntent.clientSecret,
            shippingDetails = dependencies.shippingDetails,
            draftPaymentSelection = dependencies.draftPaymentSelectionProvider(),
            autocompleteAddressInteractorFactory = dependencies.autocompleteAddressInteractorFactory,
            onAnalyticsEvent = dependencies.eventReporter::onUsBankAccountFormEvent,
            onMandateTextChanged = dependencies.onMandateTextChanged,
            onLinkedBankAccountChanged = bankFormInteractor::handleLinkedBankAccountChanged,
            onUpdatePrimaryButtonUIState = dependencies.onUpdatePrimaryButtonUIState,
            onUpdatePrimaryButtonState = dependencies.onUpdatePrimaryButtonState,
            onError = dependencies.onError,
            onFormCompleted = {
                dependencies.eventReporter.onPaymentMethodFormCompleted(PaymentMethod.Type.USBankAccount.code)
            },
            incentive = paymentMethodMetadata.paymentMethodIncentive,
            setAsDefaultPaymentMethodEnabled =
                paymentMethodMetadata.customerMetadata?.isPaymentMethodSetAsDefaultEnabled
                    ?: IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE,
            financialConnectionsAvailability = paymentMethodMetadata.financialConnectionsAvailability,
            setAsDefaultMatchesSaveForFutureUse = dependencies.setAsDefaultMatchesSaveForFutureUse,
            termsDisplay = dependencies.termsDisplayProvider(selectedPaymentMethodCode),
            sellerBusinessName = paymentMethodMetadata.sellerBusinessName,
            forceSetupFutureUseBehavior = paymentMethodMetadata.forceSetupFutureUseBehaviorAndNewMandate,
            clientAttributionMetadata = paymentMethodMetadata.clientAttributionMetadata,
        )
    }
}
