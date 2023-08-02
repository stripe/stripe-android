package com.stripe.android.paymentsheet.analytics

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
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

    override fun onInit(
        configuration: PaymentSheet.Configuration?,
        isDecoupling: Boolean,
    ) {
        fireEvent(
            PaymentSheetEvent.Init(
                mode = mode,
                configuration = configuration,
                isDecoupled = isDecoupling,
            )
        )
    }

    override fun onLoadStarted(isDecoupling: Boolean) {
        durationProvider.start(DurationProvider.Key.Loading)
        fireEvent(PaymentSheetEvent.LoadStarted(isDecoupling))
    }

    override fun onLoadSucceeded(isDecoupling: Boolean) {
        val duration = durationProvider.end(DurationProvider.Key.Loading)
        fireEvent(
            PaymentSheetEvent.LoadSucceeded(
                duration = duration,
                isDecoupled = isDecoupling,
            )
        )
    }

    override fun onLoadFailed(isDecoupling: Boolean) {
        val duration = durationProvider.end(DurationProvider.Key.Loading)
        fireEvent(
            PaymentSheetEvent.LoadFailed(
                duration = duration,
                isDecoupled = isDecoupling,
            )
        )
    }

    override fun onDismiss(
        isDecoupling: Boolean,
    ) {
        fireEvent(
            PaymentSheetEvent.Dismiss(
                isDecoupled = isDecoupling,
            )
        )
    }

    override fun onShowExistingPaymentOptions(
        linkEnabled: Boolean,
        currency: String?,
        isDecoupling: Boolean,
    ) {
        durationProvider.start(DurationProvider.Key.Checkout)

        fireEvent(
            PaymentSheetEvent.ShowExistingPaymentOptions(
                mode = mode,
                linkEnabled = linkEnabled,
                currency = currency,
                isDecoupled = isDecoupling,
            )
        )
    }

    override fun onShowNewPaymentOptionForm(
        linkEnabled: Boolean,
        currency: String?,
        isDecoupling: Boolean,
    ) {
        durationProvider.start(DurationProvider.Key.Checkout)

        fireEvent(
            PaymentSheetEvent.ShowNewPaymentOptionForm(
                mode = mode,
                linkEnabled = linkEnabled,
                currency = currency,
                isDecoupled = isDecoupling,
            )
        )
    }

    override fun onSelectPaymentMethod(
        code: PaymentMethodCode,
        currency: String?,
        isDecoupling: Boolean,
    ) {
        fireEvent(
            PaymentSheetEvent.SelectPaymentMethod(
                code = code,
                isDecoupled = isDecoupling,
                currency = currency,
            )
        )
    }

    override fun onSelectPaymentOption(
        paymentSelection: PaymentSelection,
        currency: String?,
        isDecoupling: Boolean,
    ) {
        fireEvent(
            PaymentSheetEvent.SelectPaymentOption(
                mode = mode,
                paymentSelection = paymentSelection,
                currency = currency,
                isDecoupled = isDecoupling,
            )
        )
    }

    override fun onPressConfirmButton(currency: String?, isDecoupling: Boolean) {
        fireEvent(
            PaymentSheetEvent.PressConfirmButton(
                currency = currency,
                isDecoupled = isDecoupling,
            )
        )
    }

    override fun onPaymentSuccess(
        paymentSelection: PaymentSelection?,
        currency: String?,
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
                isDecoupled = deferredIntentConfirmationType != null,
                deferredIntentConfirmationType = deferredIntentConfirmationType,
            )
        )
    }

    override fun onPaymentFailure(
        paymentSelection: PaymentSelection?,
        currency: String?,
        isDecoupling: Boolean,
    ) {
        val duration = durationProvider.end(DurationProvider.Key.Checkout)

        fireEvent(
            PaymentSheetEvent.Payment(
                mode = mode,
                paymentSelection = paymentSelection,
                duration = duration,
                result = PaymentSheetEvent.Payment.Result.Failure,
                currency = currency,
                isDecoupled = isDecoupling,
                deferredIntentConfirmationType = null,
            )
        )
    }

    override fun onLpmSpecFailure(
        isDecoupling: Boolean,
    ) {
        fireEvent(
            PaymentSheetEvent.LpmSerializeFailureEvent(isDecoupled = isDecoupling)
        )
    }

    override fun onAutofill(
        type: String,
        isDecoupling: Boolean,
    ) {
        fireEvent(
            PaymentSheetEvent.AutofillEvent(
                type = type,
                isDecoupled = isDecoupling,
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
