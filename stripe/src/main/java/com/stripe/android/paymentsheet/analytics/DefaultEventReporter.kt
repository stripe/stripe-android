package com.stripe.android.paymentsheet.analytics

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networking.AnalyticsDataFactory
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class DefaultEventReporter internal constructor(
    private val mode: EventReporter.Mode,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequest.Factory,
    private val analyticsDataFactory: AnalyticsDataFactory
) : EventReporter {

    internal constructor(
        mode: EventReporter.Mode,
        context: Context
    ) : this(
        mode,
        AnalyticsRequestExecutor.Default(),
        AnalyticsRequest.Factory(),
        AnalyticsDataFactory(
            context,
            PaymentConfiguration.getInstance(context).publishableKey
        )
    )

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

    override fun onShowExistingPaymentOptions() {
        fireEvent(
            PaymentSheetEvent.ShowExistingPaymentOptions(
                mode = mode
            )
        )
    }

    override fun onShowNewPaymentOptionForm() {
        fireEvent(
            PaymentSheetEvent.ShowNewPaymentOptionForm(
                mode = mode
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
                result = PaymentSheetEvent.Payment.Result.Success
            )
        )
    }

    override fun onPaymentFailure(paymentSelection: PaymentSelection?) {
        fireEvent(
            PaymentSheetEvent.Payment(
                mode = mode,
                paymentSelection = paymentSelection,
                result = PaymentSheetEvent.Payment.Result.Failure
            )
        )
    }

    private fun fireEvent(event: PaymentSheetEvent) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.create(
                analyticsDataFactory.createParams(event)
            )
        )
    }
}
