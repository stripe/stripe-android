package com.stripe.android.paymentsheet.analytics

import android.content.Context
import com.stripe.android.AnalyticsEvent
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
        // TODO(mshafrir-stripe): implement
    }

    override fun onDismiss() {
        // TODO(mshafrir-stripe): implement
    }

    override fun onShowExistingPaymentOptions() {
        // TODO(mshafrir-stripe): implement
    }

    override fun onShowNewPaymentOptionForm() {
        // TODO(mshafrir-stripe): implement
    }

    override fun onPaymentSuccess(paymentSelection: PaymentSelection) {
        // TODO(mshafrir-stripe): implement
    }

    override fun onPaymentFailure(paymentSelection: PaymentSelection) {
        // TODO(mshafrir-stripe): implement
    }

    private fun fireEvent(analyticsEvent: AnalyticsEvent) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.create(
                analyticsDataFactory.createParams(analyticsEvent)
            )
        )
    }
}
