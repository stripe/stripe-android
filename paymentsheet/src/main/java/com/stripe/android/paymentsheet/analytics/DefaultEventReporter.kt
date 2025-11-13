package com.stripe.android.paymentsheet.analytics

import android.content.Context
import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestV2Executor
import com.stripe.android.core.networking.AnalyticsRequestV2Factory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkDisabledReason
import com.stripe.android.model.LinkMode
import com.stripe.android.model.LinkSignupDisabledReason
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent.BankAccountCollectorFinished
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent.BankAccountCollectorStarted
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isSaved
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.WalletLocation
import com.stripe.android.paymentsheet.state.WalletsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@Suppress("LargeClass", "TooManyFunctions")
@OptIn(ExperimentalAnalyticEventCallbackApi::class)
internal class DefaultEventReporter @Inject internal constructor(
    context: Context,
    private val mode: EventReporter.Mode,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestV2Executor: AnalyticsRequestV2Executor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
    private val analyticEventCallbackProvider: Provider<AnalyticEventCallback?>,
    @IOContext private val workContext: CoroutineContext,
    private val logger: UserFacingLogger,
) : EventReporter, LoadingEventReporter {

    @Volatile
    private var state: State = State()

    private val analyticsRequestV2Factory = AnalyticsRequestV2Factory(
        context,
        clientId = CLIENT_ID,
        origin = ORIGIN,
    )

    override fun onInit(
        commonConfiguration: CommonConfiguration,
        appearance: PaymentSheet.Appearance,
        primaryButtonColor: Boolean?,
        configurationSpecificPayload: PaymentSheetEvent.ConfigurationSpecificPayload,
        isDeferred: Boolean,
    ) {
        state = state.copy(
            isDeferred = isDeferred
        )

        fireEvent(
            PaymentSheetEvent.Init(
                mode = mode,
                configuration = commonConfiguration,
                appearance = appearance,
                primaryButtonColor = primaryButtonColor,
                configurationSpecificPayload = configurationSpecificPayload,
                isDeferred = isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                isAnalyticEventCallbackSet = analyticEventCallbackProvider.get() != null,
            )
        )
    }

    override fun onLoadStarted(initializedViaCompose: Boolean) {
        durationProvider.start(DurationProvider.Key.Loading)
        fireEvent(
            PaymentSheetEvent.LoadStarted(
                isDeferred = state.isDeferred,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                isSpt = state.isSpt,
                initializedViaCompose = initializedViaCompose
            )
        )
    }

    override fun onLoadSucceeded(
        paymentSelection: PaymentSelection?,
        linkEnabled: Boolean,
        linkMode: LinkMode?,
        linkDisabledReasons: List<LinkDisabledReason>?,
        linkSignupDisabledReasons: List<LinkSignupDisabledReason>?,
        googlePaySupported: Boolean,
        linkDisplay: PaymentSheet.LinkConfiguration.Display,
        currency: String?,
        initializationMode: PaymentElementLoader.InitializationMode,
        financialConnectionsAvailability: FinancialConnectionsAvailability?,
        orderedLpms: List<String>,
        requireCvcRecollection: Boolean,
        hasDefaultPaymentMethod: Boolean?,
        setAsDefaultEnabled: Boolean?,
        paymentMethodOptionsSetupFutureUsage: Boolean,
        setupFutureUsage: StripeIntent.Usage?,
        openCardScanAutomatically: Boolean,
    ) {
        state = state.copy(
            currency = currency,
            linkEnabled = linkEnabled,
            linkMode = linkMode,
            isSpt = initializationMode is PaymentElementLoader.InitializationMode.DeferredIntent &&
                initializationMode.intentConfiguration.intentBehavior is
                PaymentSheet.IntentConfiguration.IntentBehavior.SharedPaymentToken,
            googlePaySupported = googlePaySupported,
            financialConnectionsAvailability = financialConnectionsAvailability,
        )

        durationProvider.start(DurationProvider.Key.Checkout)

        val duration = durationProvider.end(DurationProvider.Key.Loading)

        fireEvent(
            PaymentSheetEvent.LoadSucceeded(
                paymentSelection = paymentSelection,
                duration = duration,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = linkEnabled,
                linkMode = linkMode,
                linkDisabledReasons = linkDisabledReasons,
                linkSignupDisabledReasons = linkSignupDisabledReasons,
                googlePaySupported = googlePaySupported,
                linkDisplay = linkDisplay,
                initializationMode = initializationMode,
                orderedLpms = orderedLpms,
                requireCvcRecollection = requireCvcRecollection,
                hasDefaultPaymentMethod = hasDefaultPaymentMethod,
                financialConnectionsAvailability = financialConnectionsAvailability,
                setAsDefaultEnabled = setAsDefaultEnabled,
                paymentMethodOptionsSetupFutureUsage = paymentMethodOptionsSetupFutureUsage,
                setupFutureUsage = setupFutureUsage,
                openCardScanAutomatically = openCardScanAutomatically,
            )
        )
    }

    override fun onLoadFailed(
        error: Throwable,
    ) {
        val duration = durationProvider.end(DurationProvider.Key.Loading)
        fireEvent(
            PaymentSheetEvent.LoadFailed(
                duration = duration,
                error = error,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onElementsSessionLoadFailed(error: Throwable) {
        fireEvent(
            PaymentSheetEvent.ElementsSessionLoadFailed(
                error = error,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onDismiss() {
        fireEvent(
            PaymentSheetEvent.Dismiss(
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onShowExistingPaymentOptions() {
        fireAnalyticEvent(AnalyticEvent.PresentedSheet())
        fireEvent(
            PaymentSheetEvent.ShowExistingPaymentOptions(
                mode = mode,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                currency = state.currency,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
            )
        )
    }

    override fun onShowManageSavedPaymentMethods() {
        fireEvent(
            PaymentSheetEvent.ShowManagePaymentMethods(
                mode = mode,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                currency = state.currency,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
            )
        )
    }

    override fun onShowNewPaymentOptions() {
        fireAnalyticEvent(AnalyticEvent.PresentedSheet())
        fireEvent(
            PaymentSheetEvent.ShowNewPaymentOptions(
                mode = mode,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                currency = state.currency,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
            )
        )
    }

    override fun onSelectPaymentMethod(
        code: PaymentMethodCode,
    ) {
        fireAnalyticEvent(AnalyticEvent.SelectedPaymentMethodType(code))
        fireEvent(
            PaymentSheetEvent.SelectPaymentMethod(
                code = code,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                currency = state.currency,
                linkEnabled = state.linkEnabled,
                linkContext = determineLinkContextForPaymentMethodType(code),
                financialConnectionsAvailability = state.financialConnectionsAvailability,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onRemoveSavedPaymentMethod(code: PaymentMethodCode) {
        fireAnalyticEvent(AnalyticEvent.RemovedSavedPaymentMethod(code))
        fireEvent(
            PaymentSheetEvent.RemovePaymentOption(
                mode = mode,
                code = code,
                currency = state.currency,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onPaymentMethodFormShown(code: PaymentMethodCode) {
        durationProvider.start(DurationProvider.Key.ConfirmButtonClicked)

        fireAnalyticEvent(AnalyticEvent.DisplayedPaymentMethodForm(code))
        fireEvent(
            PaymentSheetEvent.ShowPaymentOptionForm(
                code = code,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onPaymentMethodFormInteraction(code: PaymentMethodCode) {
        fireAnalyticEvent(AnalyticEvent.StartedInteractionWithPaymentMethodForm(code))
        fireEvent(
            PaymentSheetEvent.PaymentOptionFormInteraction(
                code = code,
                isDeferred = state.isDeferred,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                isSpt = state.isSpt,
            )
        )
    }

    override fun onPaymentMethodFormCompleted(code: String) {
        fireAnalyticEvent(
            AnalyticEvent.CompletedPaymentMethodForm(
                paymentMethodType = code,
            )
        )
        fireEvent(
            PaymentSheetEvent.PaymentMethodFormCompleted(
                code = code,
                isDeferred = state.isDeferred,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                isSpt = state.isSpt,
            )
        )
    }

    override fun onCardNumberCompleted() {
        fireEvent(
            PaymentSheetEvent.CardNumberCompleted(
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onSelectPaymentOption(
        paymentSelection: PaymentSelection,
    ) {
        if (paymentSelection.isSaved) {
            fireAnalyticEvent(AnalyticEvent.SelectedSavedPaymentMethod(paymentSelection.code()))
        }
        fireEvent(
            PaymentSheetEvent.SelectPaymentOption(
                mode = mode,
                paymentSelection = paymentSelection,
                currency = state.currency,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onDisallowedCardBrandEntered(brand: CardBrand) {
        fireEvent(
            PaymentSheetEvent.CardBrandDisallowed(
                cardBrand = brand,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onPressConfirmButton(paymentSelection: PaymentSelection) {
        val duration = durationProvider.end(DurationProvider.Key.ConfirmButtonClicked)

        fireAnalyticEvent(AnalyticEvent.TappedConfirmButton(paymentSelection.code()))
        fireEvent(
            PaymentSheetEvent.PressConfirmButton(
                currency = state.currency,
                duration = duration,
                selectedLpm = paymentSelection.code(),
                linkContext = paymentSelection.linkContext(),
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                financialConnectionsAvailability = state.financialConnectionsAvailability,
            )
        )
    }

    override fun onPaymentSuccess(
        paymentSelection: PaymentSelection,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        isConfirmationToken: Boolean,
    ) {
        // Wallets are treated as a saved payment method after confirmation, so we need
        // to "reset" to the correct PaymentSelection for accurate reporting.
        val savedSelection = (paymentSelection as? PaymentSelection.Saved)

        val realSelection = savedSelection?.walletType?.paymentSelection ?: paymentSelection
        val duration = durationProvider.end(DurationProvider.Key.Checkout)

        fireEvent(
            PaymentSheetEvent.Payment(
                mode = mode,
                paymentSelection = realSelection,
                duration = duration,
                result = PaymentSheetEvent.Payment.Result.Success,
                currency = state.currency,
                isDeferred = deferredIntentConfirmationType != null,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                isConfirmationToken = isConfirmationToken,
                deferredIntentConfirmationType = deferredIntentConfirmationType,
            )
        )
    }

    override fun onPaymentFailure(
        paymentSelection: PaymentSelection,
        error: PaymentSheetConfirmationError,
    ) {
        val duration = durationProvider.end(DurationProvider.Key.Checkout)

        fireEvent(
            PaymentSheetEvent.Payment(
                mode = mode,
                paymentSelection = paymentSelection,
                duration = duration,
                result = PaymentSheetEvent.Payment.Result.Failure(error),
                currency = state.currency,
                isDeferred = state.isDeferred,
                isConfirmationToken = null,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                deferredIntentConfirmationType = null,
            )
        )
    }

    override fun onLpmSpecFailure(errorMessage: String?) {
        fireEvent(
            PaymentSheetEvent.LpmSerializeFailureEvent(
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                errorMessage = errorMessage
            )
        )
    }

    override fun onAutofill(
        type: String,
    ) {
        fireEvent(
            PaymentSheetEvent.AutofillEvent(
                type = type,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onShowEditablePaymentOption() {
        fireEvent(
            PaymentSheetEvent.ShowEditablePaymentOption(
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onHideEditablePaymentOption() {
        fireEvent(
            PaymentSheetEvent.HideEditablePaymentOption(
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onBrandChoiceSelected(source: EventReporter.CardBrandChoiceEventSource, selectedBrand: CardBrand) {
        fireEvent(
            PaymentSheetEvent.CardBrandSelected(
                source = when (source) {
                    EventReporter.CardBrandChoiceEventSource.Edit -> {
                        PaymentSheetEvent.CardBrandSelected.Source.Edit
                    }
                    EventReporter.CardBrandChoiceEventSource.Add -> {
                        PaymentSheetEvent.CardBrandSelected.Source.Add
                    }
                },
                selectedBrand = selectedBrand,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported
            )
        )
    }

    override fun onUpdatePaymentMethodSucceeded(
        selectedBrand: CardBrand?
    ) {
        fireEvent(
            PaymentSheetEvent.UpdatePaymentOptionSucceeded(
                selectedBrand = selectedBrand,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onUpdatePaymentMethodFailed(
        selectedBrand: CardBrand?,
        error: Throwable,
    ) {
        fireEvent(
            PaymentSheetEvent.UpdatePaymentOptionFailed(
                selectedBrand = selectedBrand,
                error = error,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onSetAsDefaultPaymentMethodSucceeded(
        paymentMethodType: String?,
    ) {
        fireEvent(
            PaymentSheetEvent.SetAsDefaultPaymentMethodSucceeded(
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                paymentMethodType = paymentMethodType,
            )
        )
    }

    override fun onExperimentExposure(
        experiment: LoggableExperiment
    ) {
        fireV2Event(
            PaymentSheetEvent.ExperimentExposure(
                experiment = experiment,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onSetAsDefaultPaymentMethodFailed(
        paymentMethodType: String?,
        error: Throwable,
    ) {
        fireEvent(
            PaymentSheetEvent.SetAsDefaultPaymentMethodFailed(
                error = error,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                paymentMethodType = paymentMethodType,
            )
        )
    }

    override fun onCannotProperlyReturnFromLinkAndOtherLPMs() {
        fireEvent(PaymentSheetEvent.CannotProperlyReturnFromLinkAndLPMs(mode = mode))
    }

    override fun onUsBankAccountFormEvent(event: USBankAccountFormViewModel.AnalyticsEvent) {
        val analyticsEvent = when (event) {
            is USBankAccountFormViewModel.AnalyticsEvent.Started -> BankAccountCollectorStarted(
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                financialConnectionsAvailability = state.financialConnectionsAvailability
            )

            is USBankAccountFormViewModel.AnalyticsEvent.Finished -> BankAccountCollectorFinished(
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                event = event,
                financialConnectionsAvailability = state.financialConnectionsAvailability,
            )
        }
        fireEvent(analyticsEvent)
    }

    override fun onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
        visiblePaymentMethods: List<String>,
        hiddenPaymentMethods: List<String>,
        walletsState: WalletsState?,
    ) {
        val isGooglePayVisible = walletsState?.googlePay(WalletLocation.HEADER) != null &&
            walletsState.buttonsEnabled
        val isLinkVisible = walletsState?.link(WalletLocation.HEADER) != null &&
            walletsState.buttonsEnabled

        val visiblePaymentMethodsWithWallets = buildList {
            if (isGooglePayVisible) add("google_pay")
            if (isLinkVisible) add("link")
            addAll(visiblePaymentMethods)
        }

        fireEvent(
            PaymentSheetEvent.InitialDisplayedPaymentMethods(
                visiblePaymentMethods = visiblePaymentMethodsWithWallets,
                hiddenPaymentMethods = hiddenPaymentMethods,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onAnalyticsEvent(event: AnalyticsEvent) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = emptyMap(),
                )
            )
        }
    }

    override fun onShopPayWebViewLoadAttempt() {
        fireEvent(
            PaymentSheetEvent.ShopPayWebviewLoadAttempt(
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onShopPayWebViewConfirmSuccess() {
        fireEvent(
            PaymentSheetEvent.ShopPayWebviewConfirmSuccess(
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onShopPayWebViewCancelled(didReceiveECEClick: Boolean) {
        fireEvent(
            PaymentSheetEvent.ShopPayWebviewCancelled(
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
                didReceiveECEClick = didReceiveECEClick,
            )
        )
    }

    override fun onCardScanStarted(implementation: String) {
        durationProvider.start(DurationProvider.Key.CardScan)
        fireEvent(
            PaymentSheetEvent.CardScanStarted(
                implementation = implementation,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onCardScanSucceeded(implementation: String) {
        val duration = durationProvider.end(DurationProvider.Key.CardScan)
        fireEvent(
            PaymentSheetEvent.CardScanSucceeded(
                implementation = implementation,
                duration = duration,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onCardScanFailed(implementation: String, error: Throwable?) {
        val duration = durationProvider.end(DurationProvider.Key.CardScan)
        fireEvent(
            PaymentSheetEvent.CardScanFailed(
                implementation = implementation,
                duration = duration,
                error = error,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onCardScanCancelled(
        implementation: String
    ) {
        val duration = durationProvider.end(DurationProvider.Key.CardScan)
        fireEvent(
            PaymentSheetEvent.CardScanCancelled(
                implementation = implementation,
                duration = duration,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onCardScanApiCheckSucceeded(implementation: String) {
        fireEvent(
            PaymentSheetEvent.CardScanApiCheckSucceeded(
                implementation = implementation,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    override fun onCardScanApiCheckFailed(implementation: String, error: Throwable?) {
        fireEvent(
            PaymentSheetEvent.CardScanApiCheckFailed(
                implementation = implementation,
                error = error,
                isDeferred = state.isDeferred,
                isSpt = state.isSpt,
                linkEnabled = state.linkEnabled,
                googlePaySupported = state.googlePaySupported,
            )
        )
    }

    private fun fireEvent(event: PaymentSheetEvent) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = event.params,
                )
            )
        }
    }

    private fun fireV2Event(event: PaymentSheetEvent) {
        CoroutineScope(workContext).launch {
            analyticsRequestV2Executor.enqueue(
                analyticsRequestV2Factory.createRequest(
                    eventName = event.eventName,
                    additionalParams = event.params,
                )
            )
        }
    }

    private fun fireAnalyticEvent(event: AnalyticEvent) {
        CoroutineScope(analyticEventCoroutineContext ?: workContext).launch {
            analyticEventCallbackProvider.get()?.run {
                try {
                    onEvent(event)
                } catch (_: Throwable) {
                    logger.logWarningWithoutPii(
                        "AnalyticEventCallback.onEvent() failed for event: $event"
                    )
                }
            }
        }
    }

    private fun determineLinkContextForPaymentMethodType(code: String): String? {
        return if (code == "link") {
            if (state.linkMode == LinkMode.LinkCardBrand) {
                "link_card_brand"
            } else {
                "instant_debits"
            }
        } else {
            null
        }
    }

    private data class State(
        val isDeferred: Boolean = false,
        val isSpt: Boolean = false,
        val linkEnabled: Boolean = false,
        val linkMode: LinkMode? = null,
        val googlePaySupported: Boolean = false,
        val currency: String? = null,
        val financialConnectionsAvailability: FinancialConnectionsAvailability? = null,
    )

    companion object {
        private const val CLIENT_ID = "stripe-mobile-sdk"
        private const val ORIGIN = "stripe-mobile-sdk-android"

        @Volatile
        var analyticEventCoroutineContext: CoroutineContext? = null
    }
}
