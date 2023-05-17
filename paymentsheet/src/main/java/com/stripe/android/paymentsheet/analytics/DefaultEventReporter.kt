package com.stripe.android.paymentsheet.analytics

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
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
    private val eventTimeProvider: EventTimeProvider,
    @IOContext private val workContext: CoroutineContext
) : EventReporter {
    private var paymentSheetShownMillis: Long? = null

    override fun onInit(
        configuration: PaymentSheet.Configuration?,
        isDecoupling: Boolean,
        isServerSideConfirmation: Boolean,
    ) {
        fireEvent(
            PaymentSheetEvent.Init(
                mode = mode,
                configuration = configuration,
                isDecoupled = isDecoupling,
                isServerSideConfirmation = isServerSideConfirmation,
            )
        )
    }

    override fun onDismiss(
        isDecoupling: Boolean,
    ) {
        fireEvent(
            PaymentSheetEvent.Dismiss(
                mode = mode,
                isDecoupled = isDecoupling,
            )
        )
    }

    override fun onShowExistingPaymentOptions(
        linkEnabled: Boolean,
        activeLinkSession: Boolean,
        currency: String?,
        isDecoupling: Boolean,
    ) {
        paymentSheetShownMillis = eventTimeProvider.currentTimeMillis()
        fireEvent(
            PaymentSheetEvent.ShowExistingPaymentOptions(
                mode = mode,
                linkEnabled = linkEnabled,
                activeLinkSession = activeLinkSession,
                currency = currency,
                isDecoupled = isDecoupling,
            )
        )
    }

    override fun onShowNewPaymentOptionForm(
        linkEnabled: Boolean,
        activeLinkSession: Boolean,
        currency: String?,
        isDecoupling: Boolean,
    ) {
        paymentSheetShownMillis = eventTimeProvider.currentTimeMillis()
        fireEvent(
            PaymentSheetEvent.ShowNewPaymentOptionForm(
                mode = mode,
                linkEnabled = linkEnabled,
                activeLinkSession = activeLinkSession,
                currency = currency,
                isDecoupled = isDecoupling,
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

    override fun onPaymentSuccess(
        paymentSelection: PaymentSelection?,
        currency: String?,
        isDecoupling: Boolean,
    ) {
        // Google Pay is treated as a saved payment method after confirmation, so we need to
        // "reset" to PaymentSelection.GooglePay for accurate reporting
        val isGooglePay = (paymentSelection as? PaymentSelection.Saved)?.isGooglePay == true

        val realSelection = if (isGooglePay) {
            PaymentSelection.GooglePay
        } else {
            paymentSelection
        }

        fireEvent(
            PaymentSheetEvent.Payment(
                mode = mode,
                paymentSelection = realSelection,
                durationMillis = durationMillisFrom(paymentSheetShownMillis),
                result = PaymentSheetEvent.Payment.Result.Success,
                currency = currency,
                isDecoupled = isDecoupling,
            )
        )
    }

    override fun onPaymentFailure(
        paymentSelection: PaymentSelection?,
        currency: String?,
        isDecoupling: Boolean,
    ) {
        fireEvent(
            PaymentSheetEvent.Payment(
                mode = mode,
                paymentSelection = paymentSelection,
                durationMillis = durationMillisFrom(paymentSheetShownMillis),
                result = PaymentSheetEvent.Payment.Result.Failure,
                currency = currency,
                isDecoupled = isDecoupling,
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

    override fun onForceSuccess() {
        fireEvent(PaymentSheetEvent.ForceSuccess)
    }

    private fun fireEvent(event: PaymentSheetEvent) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(
                    event,
                    event.additionalParams
                )
            )
        }
    }

    private fun durationMillisFrom(start: Long?) = start?.let {
        eventTimeProvider.currentTimeMillis() - it
    }?.takeIf { it > 0 }
}
