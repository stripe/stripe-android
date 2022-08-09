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

    override fun onInit(configuration: PaymentSheet.Configuration?) {
        fireEvent(
            PaymentSheetEvent.Init(
                mode = mode,
                configuration = configuration
            )
        )
    }

    override fun onDismiss() {
        fireEvent(
            PaymentSheetEvent.Dismiss(
                mode = mode
            )
        )
    }

    override fun onShowExistingPaymentOptions(linkEnabled: Boolean, activeLinkSession: Boolean) {
        paymentSheetShownMillis = eventTimeProvider.currentTimeMillis()
        fireEvent(
            PaymentSheetEvent.ShowExistingPaymentOptions(
                mode = mode,
                linkEnabled = linkEnabled,
                activeLinkSession = activeLinkSession
            )
        )
    }

    override fun onShowNewPaymentOptionForm(linkEnabled: Boolean, activeLinkSession: Boolean) {
        paymentSheetShownMillis = eventTimeProvider.currentTimeMillis()
        fireEvent(
            PaymentSheetEvent.ShowNewPaymentOptionForm(
                mode = mode,
                linkEnabled = linkEnabled,
                activeLinkSession = activeLinkSession
            )
        )
    }

    override fun onSelectPaymentOption(paymentSelection: PaymentSelection) {
        fireEvent(
            PaymentSheetEvent.SelectPaymentOption(
                mode = mode,
                paymentSelection = paymentSelection
            )
        )
    }

    override fun onPaymentSuccess(paymentSelection: PaymentSelection?) {
        fireEvent(
            PaymentSheetEvent.Payment(
                mode = mode,
                paymentSelection = paymentSelection,
                durationMillis = durationMillisFrom(paymentSheetShownMillis),
                result = PaymentSheetEvent.Payment.Result.Success
            )
        )
    }

    override fun onPaymentFailure(paymentSelection: PaymentSelection?) {
        fireEvent(
            PaymentSheetEvent.Payment(
                mode = mode,
                paymentSelection = paymentSelection,
                durationMillis = durationMillisFrom(paymentSheetShownMillis),
                result = PaymentSheetEvent.Payment.Result.Failure
            )
        )
    }

    override fun onLpmSpecFailure() {
        fireEvent(
            PaymentSheetEvent.LpmSerializeFailureEvent()
        )
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
