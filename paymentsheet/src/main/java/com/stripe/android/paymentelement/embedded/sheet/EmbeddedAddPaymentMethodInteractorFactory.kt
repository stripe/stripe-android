package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher.Companion.HOSTED_SURFACE_PAYMENT_ELEMENT
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.utils.childScope
import com.stripe.android.paymentsheet.verticalmode.BankFormInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

internal class EmbeddedAddPaymentMethodInteractorFactory @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val embeddedSelectionHolder: EmbeddedSelectionHolder,
    private val embeddedFormHelperFactory: EmbeddedFormHelperFactory,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    private val sheetActivityStateHolder: SheetActivityStateHolder,
    private val tapToAddHelper: TapToAddHelper,
    private val eventReporter: EventReporter,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper,
    private val customerStateHolder: CustomerStateHolder,
    private val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory,
) {
    @Suppress("LongMethod")
    fun create(): AddPaymentMethodInteractor {
        val coroutineScope = viewModelScope.childScope(Dispatchers.Main)
        val initialCode = (embeddedSelectionHolder.selection.value as? PaymentSelection.New)?.paymentMethodType
            ?: paymentMethodMetadata.supportedPaymentMethodTypes().first()
        val hasSavedPaymentMethods = customerStateHolder.paymentMethods.value.isNotEmpty()

        val formHelper = embeddedFormHelperFactory.create(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = eventReporter,
            automaticallyLaunchedCardScanFormDataHelper =
                embeddedFormHelperFactory.createAutomaticallyLaunchedCardScanFormDataHelper(
                    selectedPaymentMethodCode = initialCode,
                    paymentMethodMetadata = paymentMethodMetadata,
                ),
            selectionUpdater = { embeddedSelectionHolder.setSelection(it) },
            tapToAddHelper = tapToAddHelper,
            // If no saved payment methods, then first saved payment method is automatically set as default
            setAsDefaultMatchesSaveForFutureUse = !hasSavedPaymentMethods,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
        )
        val bankFormInteractor = BankFormInteractor(
            updateSelection = embeddedSelectionHolder::setSelection,
            paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
                paymentMethodMetadata.paymentMethodIncentive
            ),
        )

        return DefaultAddPaymentMethodInteractor(
            initiallySelectedPaymentMethodType = initialCode,
            selection = embeddedSelectionHolder.selection,
            processing = sheetActivityStateHolder.state.mapAsStateFlow { it.isProcessing },
            validationRequested = sheetActivityStateHolder.validationRequested,
            incentive = bankFormInteractor.paymentMethodIncentiveInteractor.displayedIncentive,
            supportedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods(),
            createFormArguments = formHelper::createFormArguments,
            formElementsForCode = formHelper::formElementsForCode,
            clearErrorMessages = { sheetActivityStateHolder.updateError(null) },
            reportFieldInteraction = eventReporter::onPaymentMethodFormInteraction,
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            reportPaymentMethodTypeSelected = eventReporter::onSelectPaymentMethod,
            reportPromotionDisplayed = { code ->
                paymentMethodMessagePromotionsHelper.reportPromotionDisplayed(code, paymentMethodMetadata)
            },
            createUSBankAccountFormArguments = { code ->
                createUsBankAccountFormArguments(code, hasSavedPaymentMethods, bankFormInteractor)
            },
            coroutineScope = coroutineScope,
            uiContext = Dispatchers.Main,
            onInitiallyDisplayedPaymentMethodVisibilitySnapshot = { visiblePaymentMethods, hiddenPaymentMethods ->
                eventReporter.onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
                    visiblePaymentMethods = visiblePaymentMethods,
                    hiddenPaymentMethods = hiddenPaymentMethods,
                    walletsState = null,
                    isVerticalLayout = false,
                )
            },
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
        )
    }

    private fun createUsBankAccountFormArguments(
        paymentMethodCode: PaymentMethodCode,
        hasSavedPaymentMethods: Boolean,
        bankFormInteractor: BankFormInteractor,
    ): USBankAccountFormArguments {
        return USBankAccountFormArguments.createForEmbedded(
            paymentMethodMetadata = paymentMethodMetadata,
            selectedPaymentMethodCode = paymentMethodCode,
            hostedSurface = HOSTED_SURFACE_PAYMENT_ELEMENT,
            isCompleteFlow = false,
            draftPaymentSelection = null,
            bankFormInteractor = bankFormInteractor,
            hasSavedPaymentMethods = hasSavedPaymentMethods,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
            onAnalyticsEvent = eventReporter::onUsBankAccountFormEvent,
            onMandateTextChanged = { mandateText, _ ->
                sheetActivityStateHolder.updateMandate(mandateText)
            },
            onUpdatePrimaryButtonUIState = sheetActivityStateHolder::updatePrimaryButton,
            onError = sheetActivityStateHolder::updateError,
            onFormCompleted = { eventReporter.onPaymentMethodFormCompleted(PaymentMethod.Type.USBankAccount.code) },
        )
    }
}
