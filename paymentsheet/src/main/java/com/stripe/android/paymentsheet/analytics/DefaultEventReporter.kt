package com.stripe.android.paymentsheet.analytics

import android.content.Context
import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestV2Executor
import com.stripe.android.core.networking.AnalyticsRequestV2Factory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent.BankAccountCollectorFinished
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent.BankAccountCollectorStarted
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isSaved
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel
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
    private val paymentMethodMetadataProvider: Provider<PaymentMethodMetadata?>,
) : EventReporter, LoadingEventReporter {

    private val analyticsRequestV2Factory = AnalyticsRequestV2Factory(
        context,
        clientId = CLIENT_ID,
        origin = ORIGIN,
    )

    override fun onInit() {
        fireEvent(
            event = PaymentSheetEvent.Init(
                mode = mode,
            ),
            paymentMethodMetadata = null, // We won't have a value on init, and using null prevents a stack overflow.
        )
    }

    override fun onLoadStarted(initializedViaCompose: Boolean) {
        durationProvider.start(DurationProvider.Key.Loading)
        fireEvent(
            event = PaymentSheetEvent.LoadStarted(
                initializedViaCompose = initializedViaCompose
            ),
            paymentMethodMetadata = null, // We don't have these details until load is complete.
        )
    }

    override fun onLoadSucceeded(
        paymentSelection: PaymentSelection?,
        paymentMethodMetadata: PaymentMethodMetadata,
    ) {
        durationProvider.start(DurationProvider.Key.Checkout)

        val duration = durationProvider.end(DurationProvider.Key.Loading)

        fireEvent(
            event = PaymentSheetEvent.LoadSucceeded(
                paymentSelection = paymentSelection,
                duration = duration,
                orderedLpms = paymentMethodMetadata.sortedSupportedPaymentMethods().map { it.code }
            ),
            paymentMethodMetadata = paymentMethodMetadata,
        )
    }

    override fun onLoadFailed(
        error: Throwable,
    ) {
        val duration = durationProvider.end(DurationProvider.Key.Loading)
        fireEvent(
            event = PaymentSheetEvent.LoadFailed(
                duration = duration,
                error = error,
            ),
            paymentMethodMetadata = null, // We don't have these details until load is completed successfully.
        )
    }

    override fun onElementsSessionLoadFailed(error: Throwable) {
        fireEvent(
            event = PaymentSheetEvent.ElementsSessionLoadFailed(
                error = error,
            ),
            paymentMethodMetadata = null, // We don't have these details until load is completed successfully.
        )
    }

    override fun onDismiss() {
        fireEvent(PaymentSheetEvent.Dismiss())
    }

    override fun onShowExistingPaymentOptions() {
        fireAnalyticEvent(AnalyticEvent.PresentedSheet())
        fireEvent(
            PaymentSheetEvent.ShowExistingPaymentOptions(
                mode = mode,
            )
        )
    }

    override fun onShowManageSavedPaymentMethods() {
        fireEvent(
            PaymentSheetEvent.ShowManagePaymentMethods(
                mode = mode,
            )
        )
    }

    override fun onShowNewPaymentOptions() {
        fireAnalyticEvent(AnalyticEvent.PresentedSheet())
        fireEvent(
            PaymentSheetEvent.ShowNewPaymentOptions(
                mode = mode,
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
                linkContext = determineLinkContextForPaymentMethodType(code),
            )
        )
    }

    override fun onRemoveSavedPaymentMethod(code: PaymentMethodCode) {
        fireAnalyticEvent(AnalyticEvent.RemovedSavedPaymentMethod(code))
        fireEvent(
            PaymentSheetEvent.RemovePaymentOption(
                mode = mode,
                code = code,
            )
        )
    }

    override fun onPaymentMethodFormShown(code: PaymentMethodCode) {
        durationProvider.start(DurationProvider.Key.ConfirmButtonClicked)

        fireAnalyticEvent(AnalyticEvent.DisplayedPaymentMethodForm(code))
        fireEvent(
            PaymentSheetEvent.ShowPaymentOptionForm(
                code = code,
            )
        )
    }

    override fun onPaymentMethodFormInteraction(code: PaymentMethodCode) {
        fireAnalyticEvent(AnalyticEvent.StartedInteractionWithPaymentMethodForm(code))
        fireEvent(
            PaymentSheetEvent.PaymentOptionFormInteraction(
                code = code,
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
            )
        )
    }

    override fun onCardNumberCompleted() {
        fireEvent(PaymentSheetEvent.CardNumberCompleted())
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
            )
        )
    }

    override fun onDisallowedCardBrandEntered(brand: CardBrand) {
        fireEvent(
            PaymentSheetEvent.CardBrandDisallowed(
                cardBrand = brand,
            )
        )
    }

    override fun onPressConfirmButton(paymentSelection: PaymentSelection) {
        val duration = durationProvider.end(DurationProvider.Key.ConfirmButtonClicked)

        fireAnalyticEvent(AnalyticEvent.TappedConfirmButton(paymentSelection.code()))
        fireEvent(
            PaymentSheetEvent.PressConfirmButton(
                duration = duration,
                selectedLpm = paymentSelection.code(),
                linkContext = paymentSelection.linkContext(),
            )
        )
    }

    override fun onPaymentSuccess(
        paymentSelection: PaymentSelection,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intentId: String?,
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
                deferredIntentConfirmationType = deferredIntentConfirmationType,
                intentId = intentId,
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
                deferredIntentConfirmationType = null,
                intentId = null,
            )
        )
    }

    override fun onLpmSpecFailure(errorMessage: String?) {
        fireEvent(
            event = PaymentSheetEvent.LpmSerializeFailureEvent(
                errorMessage = errorMessage
            ),
            paymentMethodMetadata = null, // We don't have these details until load is completed successfully.
        )
    }

    override fun onAutofill(
        type: String,
    ) {
        fireEvent(
            PaymentSheetEvent.AutofillEvent(
                type = type,
            )
        )
    }

    override fun onShowEditablePaymentOption() {
        fireEvent(PaymentSheetEvent.ShowEditablePaymentOption())
    }

    override fun onHideEditablePaymentOption() {
        fireEvent(PaymentSheetEvent.HideEditablePaymentOption())
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
            )
        )
    }

    override fun onUpdatePaymentMethodSucceeded(
        selectedBrand: CardBrand?
    ) {
        fireEvent(
            PaymentSheetEvent.UpdatePaymentOptionSucceeded(
                selectedBrand = selectedBrand,
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
            )
        )
    }

    override fun onSetAsDefaultPaymentMethodSucceeded(
        paymentMethodType: String?,
    ) {
        fireEvent(
            PaymentSheetEvent.SetAsDefaultPaymentMethodSucceeded(
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
                paymentMethodType = paymentMethodType,
            )
        )
    }

    override fun onCannotProperlyReturnFromLinkAndOtherLPMs() {
        fireEvent(PaymentSheetEvent.CannotProperlyReturnFromLinkAndLPMs(mode = mode))
    }

    override fun onUsBankAccountFormEvent(event: USBankAccountFormViewModel.AnalyticsEvent) {
        val analyticsEvent = when (event) {
            is USBankAccountFormViewModel.AnalyticsEvent.Started -> BankAccountCollectorStarted()

            is USBankAccountFormViewModel.AnalyticsEvent.Finished -> BankAccountCollectorFinished(
                event = event,
            )
        }
        fireEvent(analyticsEvent)
    }

    override fun onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
        visiblePaymentMethods: List<String>,
        hiddenPaymentMethods: List<String>,
        walletsState: WalletsState?,
        isVerticalLayout: Boolean,
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
                isVerticalLayout = isVerticalLayout,
            )
        )
    }

    override fun onAnalyticsEvent(event: AnalyticsEvent) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = defaultParams(paymentMethodMetadataProvider.get()),
                )
            )
        }
    }

    override fun onShopPayWebViewLoadAttempt() {
        fireEvent(PaymentSheetEvent.ShopPayWebviewLoadAttempt())
    }

    override fun onShopPayWebViewConfirmSuccess() {
        fireEvent(PaymentSheetEvent.ShopPayWebviewConfirmSuccess())
    }

    override fun onShopPayWebViewCancelled(didReceiveECEClick: Boolean) {
        fireEvent(
            PaymentSheetEvent.ShopPayWebviewCancelled(
                didReceiveECEClick = didReceiveECEClick,
            )
        )
    }

    override fun onCardScanStarted(implementation: String) {
        durationProvider.start(DurationProvider.Key.CardScan)
        fireEvent(
            PaymentSheetEvent.CardScanStarted(
                implementation = implementation,
            )
        )
    }

    override fun onCardScanSucceeded(implementation: String) {
        val duration = durationProvider.end(DurationProvider.Key.CardScan)
        fireEvent(
            PaymentSheetEvent.CardScanSucceeded(
                implementation = implementation,
                duration = duration,
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
            )
        )
    }

    override fun onCardScanApiCheckSucceeded(implementation: String) {
        fireEvent(
            PaymentSheetEvent.CardScanApiCheckSucceeded(
                implementation = implementation,
            )
        )
    }

    override fun onCardScanApiCheckFailed(implementation: String, error: Throwable?) {
        fireEvent(
            PaymentSheetEvent.CardScanApiCheckFailed(
                implementation = implementation,
                error = error,
            )
        )
        error?.message?.let {
            logger.logWarningWithoutPii("Card scan check failed: $it")
        }
    }

    private fun defaultParams(paymentMethodMetadata: PaymentMethodMetadata?): Map<String, Any> {
        return paymentMethodMetadata?.analyticsMetadata?.paramsMap ?: emptyMap()
    }

    private fun fireEvent(
        event: PaymentSheetEvent,
        paymentMethodMetadata: PaymentMethodMetadata? = paymentMethodMetadataProvider.get(),
    ) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = defaultParams(paymentMethodMetadata) + event.params,
                )
            )
        }
    }

    private fun fireV2Event(event: PaymentSheetEvent) {
        CoroutineScope(workContext).launch {
            val paymentMethodMetadata = paymentMethodMetadataProvider.get()
            analyticsRequestV2Executor.enqueue(
                analyticsRequestV2Factory.createRequest(
                    eventName = event.eventName,
                    additionalParams = defaultParams(paymentMethodMetadata) + event.params,
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
            if (paymentMethodMetadataProvider.get()?.linkMode == LinkMode.LinkCardBrand) {
                "link_card_brand"
            } else {
                "instant_debits"
            }
        } else {
            null
        }
    }

    companion object {
        private const val CLIENT_ID = "stripe-mobile-sdk"
        private const val ORIGIN = "stripe-mobile-sdk-android"

        @Volatile
        var analyticEventCoroutineContext: CoroutineContext? = null
    }
}
