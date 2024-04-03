package com.stripe.android.paymentsheet.analytics

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentsheet.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class DefaultEventReporter @Inject internal constructor(
    private val mode: EventReporter.Mode,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
    @IOContext private val workContext: CoroutineContext
) : EventReporter {

    private var isDeferred: Boolean = false
    private var linkEnabled: Boolean = false
    private var googlePaySupported: Boolean = false
    private var currency: String? = null

    override fun onInit(
        configuration: PaymentSheet.Configuration,
        isDeferred: Boolean,
    ) {
        this.isDeferred = isDeferred

        fireEvent(
            PaymentSheetEvent.Init(
                mode = mode,
                configuration = configuration,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onLoadStarted() {
        durationProvider.start(DurationProvider.Key.Loading)
        fireEvent(PaymentSheetEvent.LoadStarted(isDeferred, linkEnabled, googlePaySupported))
    }

    override fun onLoadSucceeded(
        paymentSelection: PaymentSelection?,
        linkEnabled: Boolean,
        googlePaySupported: Boolean,
        currency: String?,
    ) {
        this.currency = currency
        this.linkEnabled = linkEnabled
        this.googlePaySupported = googlePaySupported

        durationProvider.start(DurationProvider.Key.Checkout)

        val duration = durationProvider.end(DurationProvider.Key.Loading)

        fireEvent(
            PaymentSheetEvent.LoadSucceeded(
                paymentSelection = paymentSelection,
                duration = duration,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
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

    override fun onShowNewPaymentOptionForm() {
        fireEvent(
            PaymentSheetEvent.ShowNewPaymentOptionForm(
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
    ) {
        fireEvent(
            PaymentSheetEvent.SelectPaymentMethod(
                code = code,
                isDeferred = isDeferred,
                currency = currency,
                linkEnabled = linkEnabled,
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

    override fun onPressConfirmButton(paymentSelection: PaymentSelection?,) {
        val duration = durationProvider.end(DurationProvider.Key.ConfirmButtonClicked)

        fireEvent(
            PaymentSheetEvent.PressConfirmButton(
                currency = currency,
                duration = duration,
                selectedLpm = paymentSelection.code(),
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

    override fun onShowPaymentOptionBrands(
        source: EventReporter.CardBrandChoiceEventSource,
        selectedBrand: CardBrand
    ) {
        fireEvent(
            PaymentSheetEvent.ShowPaymentOptionBrands(
                selectedBrand = selectedBrand,
                source = when (source) {
                    EventReporter.CardBrandChoiceEventSource.Add ->
                        PaymentSheetEvent.ShowPaymentOptionBrands.Source.Add
                    EventReporter.CardBrandChoiceEventSource.Edit ->
                        PaymentSheetEvent.ShowPaymentOptionBrands.Source.Edit
                },
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onHidePaymentOptionBrands(
        source: EventReporter.CardBrandChoiceEventSource,
        selectedBrand: CardBrand?
    ) {
        fireEvent(
            PaymentSheetEvent.HidePaymentOptionBrands(
                selectedBrand = selectedBrand,
                source = when (source) {
                    EventReporter.CardBrandChoiceEventSource.Add ->
                        PaymentSheetEvent.HidePaymentOptionBrands.Source.Add
                    EventReporter.CardBrandChoiceEventSource.Edit ->
                        PaymentSheetEvent.HidePaymentOptionBrands.Source.Edit
                },
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
                googlePaySupported = googlePaySupported,
            )
        )
    }

    override fun onUpdatePaymentMethodSucceeded(
        selectedBrand: CardBrand
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
        selectedBrand: CardBrand,
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
}
