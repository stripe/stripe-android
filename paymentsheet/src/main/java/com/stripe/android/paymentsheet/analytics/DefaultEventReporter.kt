package com.stripe.android.paymentsheet.analytics

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentsheet.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent.CardBrandChoiceDropdownClosed
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent.CardBrandChoiceDropdownDisplayed
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent.CardBrandChoiceDropdownOpened
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
            )
        )
    }

    override fun onLoadStarted() {
        durationProvider.start(DurationProvider.Key.Loading)
        fireEvent(PaymentSheetEvent.LoadStarted(isDeferred))
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
            )
        )
    }

    override fun onElementsSessionLoadFailed(error: Throwable) {
        fireEvent(
            PaymentSheetEvent.ElementsSessionLoadFailed(
                error = error.asPaymentSheetLoadingException.type,
                isDeferred = isDeferred,
            )
        )
    }

    override fun onDismiss() {
        fireEvent(
            PaymentSheetEvent.Dismiss(
                isDeferred = isDeferred,
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
            )
        )
    }

    override fun onPressConfirmButton() {
        fireEvent(
            PaymentSheetEvent.PressConfirmButton(
                currency = currency,
                isDeferred = isDeferred,
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
                deferredIntentConfirmationType = null,
            )
        )
    }

    override fun onLpmSpecFailure() {
        fireEvent(
            PaymentSheetEvent.LpmSerializeFailureEvent(isDeferred = isDeferred)
        )
    }

    override fun onAutofill(
        type: String,
    ) {
        fireEvent(
            PaymentSheetEvent.AutofillEvent(
                type = type,
                isDeferred = isDeferred,
            )
        )
    }

    override fun onCardBrandChoiceDropdownDisplayed() {
        fireEvent(CardBrandChoiceDropdownDisplayed(isDeferred))
    }

    override fun onCardBrandChoiceDropdownOpened(initialSelection: String?) {
        fireEvent(CardBrandChoiceDropdownOpened(initialSelection, isDeferred))
    }

    override fun onCardBrandChoiceDropdownClosed(selection: String?) {
        fireEvent(CardBrandChoiceDropdownClosed(selection, isDeferred))
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
