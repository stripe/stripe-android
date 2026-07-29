package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
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
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.job
import javax.inject.Inject

/**
 * Builds the [AddPaymentMethodInteractor] backing the horizontal payment-options screen. Mirrors
 * [com.stripe.android.paymentelement.embedded.form.EmbeddedFormInteractorFactory], constructing the interactor
 * without a [com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel] by sourcing every dependency from the
 * embedded holders.
 */
internal class EmbeddedAddPaymentMethodInteractorFactory @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val embeddedSelectionHolder: EmbeddedSelectionHolder,
    private val embeddedFormHelperFactory: EmbeddedFormHelperFactory,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    private val sheetActivityStateHolder: SheetActivityStateHolder,
    private val tapToAddHelper: TapToAddHelper,
    private val eventReporter: EventReporter,
    private val customerStateHolder: CustomerStateHolder,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper,
) {
    fun create(): AddPaymentMethodInteractor {
        val formScope = CoroutineScope(
            viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext.job)
        )
        val initialCode = (embeddedSelectionHolder.selection.value as? PaymentSelection.New)?.paymentMethodType
            ?: paymentMethodMetadata.supportedPaymentMethodTypes().first()
        val hasSavedPaymentMethods = customerStateHolder.paymentMethods.value.isNotEmpty()

        // Horizontal mode renders the form inline, so it uses the full form helper (card scan auto-launch +
        // tap-to-add), matching the main PaymentSheet's horizontal AddPaymentMethod flow. A single card-scan helper
        // is built for the initially selected code, as in the main flow.
        val formHelper = embeddedFormHelperFactory.create(
            coroutineScope = formScope,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = eventReporter,
            automaticallyLaunchedCardScanFormDataHelper =
                embeddedFormHelperFactory.createAutomaticallyLaunchedCardScanFormDataHelper(
                    selectedPaymentMethodCode = initialCode,
                    paymentMethodMetadata = paymentMethodMetadata,
                ),
            selectionUpdater = { embeddedSelectionHolder.setSelection(it) },
            tapToAddHelper = tapToAddHelper,
            setAsDefaultMatchesSaveForFutureUse = !hasSavedPaymentMethods,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
        )

        return DefaultAddPaymentMethodInteractor(
            initiallySelectedPaymentMethodType = initialCode,
            selection = embeddedSelectionHolder.selection,
            processing = sheetActivityStateHolder.state.mapAsStateFlow { it.isProcessing },
            // Embedded does not support validation at the moment. Should update here once it does.
            validationRequested = MutableSharedFlow(),
            incentive = PaymentMethodIncentiveInteractor(
                paymentMethodMetadata.paymentMethodIncentive
            ).displayedIncentive,
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
                createUsBankAccountFormArguments(code)
            },
            coroutineScope = formScope,
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

    private fun createUsBankAccountFormArguments(code: String): USBankAccountFormArguments {
        return USBankAccountFormArguments.createForEmbedded(
            paymentMethodMetadata = paymentMethodMetadata,
            selectedPaymentMethodCode = code,
            hostedSurface = HOSTED_SURFACE_PAYMENT_ELEMENT,
            setSelection = embeddedSelectionHolder::setSelection,
            hasSavedPaymentMethods = customerStateHolder.paymentMethods.value.any { it.type?.code == code },
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
