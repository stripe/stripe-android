package com.stripe.android.paymentsheet.analytics

import android.content.Context
import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestV2Executor
import com.stripe.android.core.networking.AnalyticsRequestV2Factory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.ui.core.IsStripeCardScanAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

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
    private val isStripeCardScanAvailable: IsStripeCardScanAvailable,
    private val logger: UserFacingLogger,
) : EventReporter {

    private var isDeferred: Boolean = false
    private var linkEnabled: Boolean = false
    private var linkMode: LinkMode? = null
    private var googlePaySupported: Boolean = false
    private var currency: String? = null

    private val analyticsRequestV2Factory = AnalyticsRequestV2Factory(
        context,
        clientId = CLIENT_ID,
        origin = ORIGIN,
    )

    override fun onInit(
        commonConfiguration: CommonConfiguration,
        appearance: PaymentSheet.Appearance,
        primaryButtonColor: Boolean?,
        paymentMethodLayout: PaymentSheet.PaymentMethodLayout?,
        isDeferred: Boolean,
    ) {
        this.isDeferred = isDeferred

        fireEvent(
            PaymentSheetEvent.Init(
                mode = mode,
                configuration = commonConfiguration,
                appearance = appearance,
                primaryButtonColor = primaryButtonColor,
                paymentMethodLayout = paymentMethodLayout,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
                isStripeCardScanAvailable = isStripeCardScanAvailable(),
            )
        )
    }

    override fun onLoadStarted(initializedViaCompose: Boolean) {
        durationProvider.start(DurationProvider.Key.Loading)
        fireEvent(PaymentSheetEvent.LoadStarted(isDeferred, linkEnabled, googlePaySupported, initializedViaCompose))
    }

    override fun onLoadSucceeded(
        paymentSelection: PaymentSelection?,
        linkEnabled: Boolean,
        linkMode: LinkMode?,
        googlePaySupported: Boolean,
        linkDisplay: PaymentSheet.LinkConfiguration.Display,
        currency: String?,
        initializationMode: PaymentElementLoader.InitializationMode,
        orderedLpms: List<String>,
        requireCvcRecollection: Boolean,
        hasDefaultPaymentMethod: Boolean?,
        setAsDefaultEnabled: Boolean?,
    ) {
        this.currency = currency
        this.linkEnabled = linkEnabled
        this.linkMode = linkMode
        this.googlePaySupported = googlePaySupported

        durationProvider.start(DurationProvider.Key.Checkout)

        val duration = durationProvider.end(DurationProvider.Key.Loading)

        fireEvent(
            PaymentSheetEvent.LoadSucceeded(
                paymentSelection = paymentSelection,
                duration = duration,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                linkMode = linkMode,
                googlePaySupported = googlePaySupported,
                linkDisplay = linkDisplay,
                initializationMode = initializationMode,
                orderedLpms = orderedLpms,
                requireCvcRecollection = requireCvcRecollection,
                hasDefaultPaymentMethod = hasDefaultPaymentMethod,
                setAsDefaultEnabled = setAsDefaultEnabled,
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
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onElementsSessionLoadFailed(error: Throwable) {
        fireEvent(
            PaymentSheetEvent.ElementsSessionLoadFailed(
                error = error,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onDismiss() {
        fireEvent(
            PaymentSheetEvent.Dismiss(
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onShowExistingPaymentOptions() {
        fireEvent(
            PaymentSheetEvent.ShowExistingPaymentOptions(
                mode = mode,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
                currency = currency,
                isDeferred = isDeferred,
            )
        )
    }

    override fun onShowManageSavedPaymentMethods() {
        fireEvent(
            PaymentSheetEvent.ShowManagePaymentMethods(
                mode = mode,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
                currency = currency,
                isDeferred = isDeferred,
            )
        )
    }

    override fun onShowNewPaymentOptions() {
        fireAnalyticEvent(AnalyticEvent.PresentedSheet())
        fireEvent(
            PaymentSheetEvent.ShowNewPaymentOptions(
                mode = mode,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
                currency = currency,
                isDeferred = isDeferred,
            )
        )
    }

    override fun onSelectPaymentMethod(
        code: PaymentMethodCode,
        isSaved: Boolean,
    ) {
        if (isSaved) {
            fireAnalyticEvent(AnalyticEvent.SelectedSavedPaymentMethod(code))
        }
        fireEvent(
            PaymentSheetEvent.SelectPaymentMethod(
                code = if (isSaved) "saved" else code,
                isDeferred = isDeferred,
                currency = currency,
                linkEnabled = linkEnabled,
                linkContext = determineLinkContextForPaymentMethodType(code),
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onPaymentMethodFormShown(code: PaymentMethodCode) {
        durationProvider.start(DurationProvider.Key.ConfirmButtonClicked)

        fireEvent(
            PaymentSheetEvent.ShowPaymentOptionForm(
                code = code,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onPaymentMethodFormInteraction(code: PaymentMethodCode) {
        fireEvent(
            PaymentSheetEvent.PaymentOptionFormInteraction(
                code = code,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onPaymentMethodFormCompleted(code: String) {
        fireAnalyticEvent(
            AnalyticEvent.CompletedPaymentMethodForm(
                paymentMethodType = code,
            )
        )
    }

    override fun onCardNumberCompleted() {
        fireEvent(
            PaymentSheetEvent.CardNumberCompleted(
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onSelectPaymentOption(
        paymentSelection: PaymentSelection,
    ) {
        fireEvent(
            PaymentSheetEvent.SelectPaymentOption(
                mode = mode,
                paymentSelection = paymentSelection,
                currency = currency,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onDisallowedCardBrandEntered(brand: CardBrand) {
        fireEvent(
            PaymentSheetEvent.CardBrandDisallowed(
                cardBrand = brand,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onPressConfirmButton(paymentSelection: PaymentSelection?) {
        val duration = durationProvider.end(DurationProvider.Key.ConfirmButtonClicked)

        fireEvent(
            PaymentSheetEvent.PressConfirmButton(
                currency = currency,
                duration = duration,
                selectedLpm = paymentSelection.code(),
                linkContext = paymentSelection.linkContext(),
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onPaymentSuccess(
        paymentSelection: PaymentSelection?,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
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
                currency = currency,
                isDeferred = deferredIntentConfirmationType != null,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
                deferredIntentConfirmationType = deferredIntentConfirmationType,
            )
        )
    }

    override fun onPaymentFailure(
        paymentSelection: PaymentSelection?,
        error: PaymentSheetConfirmationError,
    ) {
        val duration = durationProvider.end(DurationProvider.Key.Checkout)

        fireEvent(
            PaymentSheetEvent.Payment(
                mode = mode,
                paymentSelection = paymentSelection,
                duration = duration,
                result = PaymentSheetEvent.Payment.Result.Failure(error),
                currency = currency,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
                deferredIntentConfirmationType = null,
            )
        )
    }

    override fun onLpmSpecFailure(errorMessage: String?) {
        fireEvent(
            PaymentSheetEvent.LpmSerializeFailureEvent(
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
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
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onShowEditablePaymentOption() {
        fireEvent(
            PaymentSheetEvent.ShowEditablePaymentOption(
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onHideEditablePaymentOption() {
        fireEvent(
            PaymentSheetEvent.HideEditablePaymentOption(
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
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
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported
            )
        )
    }

    override fun onUpdatePaymentMethodSucceeded(
        selectedBrand: CardBrand?
    ) {
        fireEvent(
            PaymentSheetEvent.UpdatePaymentOptionSucceeded(
                selectedBrand = selectedBrand,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
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
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onSetAsDefaultPaymentMethodSucceeded(
        paymentMethodType: String?,
    ) {
        fireEvent(
            PaymentSheetEvent.SetAsDefaultPaymentMethodSucceeded(
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
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
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
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
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
                paymentMethodType = paymentMethodType,
            )
        )
    }

    override fun onCannotProperlyReturnFromLinkAndOtherLPMs() {
        fireEvent(PaymentSheetEvent.CannotProperlyReturnFromLinkAndLPMs(mode = mode))
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
        CoroutineScope(workContext).launch {
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
            if (linkMode == LinkMode.LinkCardBrand) {
                "link_card_brand"
            } else {
                "instant_debits"
            }
        } else {
            null
        }
    }

    private companion object {
        const val CLIENT_ID = "stripe-mobile-sdk"
        const val ORIGIN = "stripe-mobile-sdk-android"
    }
}
