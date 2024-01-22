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
import com.stripe.android.paymentsheet.state.asPaymentSheetLoadingException
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
            )
        )
    }

    override fun onLoadStarted() {
        durationProvider.start(DurationProvider.Key.Loading)
        fireEvent(PaymentSheetEvent.LoadStarted(isDeferred, linkEnabled))
    }

    override fun onLoadSucceeded(
        linkEnabled: Boolean,
        currency: String?,
    ) {
        this.currency = currency
        this.linkEnabled = linkEnabled

        val duration = durationProvider.end(DurationProvider.Key.Loading)

        fireEvent(
            PaymentSheetEvent.LoadSucceeded(
                duration = duration,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
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
                error = error.asPaymentSheetLoadingException.type,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
            )
        )
    }

    override fun onElementsSessionLoadFailed(error: Throwable) {
        fireEvent(
            PaymentSheetEvent.ElementsSessionLoadFailed(
                error = error.asPaymentSheetLoadingException.type,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
            )
        )
    }

    override fun onDismiss() {
        fireEvent(
            PaymentSheetEvent.Dismiss(
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
            )
        )
    }

    override fun onShowExistingPaymentOptions() {
        durationProvider.start(DurationProvider.Key.Checkout)

        fireEvent(
            PaymentSheetEvent.ShowExistingPaymentOptions(
                mode = mode,
                linkEnabled = linkEnabled,
                currency = currency,
                isDeferred = isDeferred,
            )
        )
    }

    override fun onShowNewPaymentOptionForm() {
        durationProvider.start(DurationProvider.Key.Checkout)

        fireEvent(
            PaymentSheetEvent.ShowNewPaymentOptionForm(
                mode = mode,
                linkEnabled = linkEnabled,
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
            )
        )
    }

    override fun onPressConfirmButton() {
        fireEvent(
            PaymentSheetEvent.PressConfirmButton(
                currency = currency,
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
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
                deferredIntentConfirmationType = null,
            )
        )
    }

    override fun onLpmSpecFailure() {
        fireEvent(
            PaymentSheetEvent.LpmSerializeFailureEvent(
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
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
            )
        )
    }

    override fun onShowEditablePaymentOption() {
        fireEvent(
            PaymentSheetEvent.ShowEditablePaymentOption(
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
            )
        )
    }

    override fun onHideEditablePaymentOption() {
        fireEvent(
            PaymentSheetEvent.HideEditablePaymentOption(
                isDeferred = isDeferred,
                linkEnabled = linkEnabled,
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
}
