package com.stripe.android.paymentsheet.analytics

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networking.AnalyticsDataFactory
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class DefaultEventReporter internal constructor(
    private val mode: EventReporter.Mode,
    private val sessionId: SessionId?,
    private val deviceIdRepository: DeviceIdRepository,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequest.Factory,
    private val analyticsDataFactory: AnalyticsDataFactory,
    private val workContext: CoroutineContext
) : EventReporter {

    internal constructor(
        mode: EventReporter.Mode,
        sessionId: SessionId?,
        context: Context,
        workContext: CoroutineContext = Dispatchers.IO
    ) : this(
        mode,
        sessionId,
        DefaultDeviceIdRepository(context, workContext),
        AnalyticsRequestExecutor.Default(),
        AnalyticsRequest.Factory(),
        AnalyticsDataFactory(
            context,
            PaymentConfiguration.getInstance(context).publishableKey
        ),
        workContext
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
        CoroutineScope(workContext).launch {
            val deviceId = deviceIdRepository.get()
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.create(
                    analyticsDataFactory.createParams(
                        event,
                        sessionId,
                        deviceId
                    )
                )
            )
        }
    }
}
