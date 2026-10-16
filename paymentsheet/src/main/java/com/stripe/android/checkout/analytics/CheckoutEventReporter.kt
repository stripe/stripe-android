package com.stripe.android.checkout.analytics

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.paymentsheet.analytics.defaultAnalyticsParams
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal class CheckoutEventReporter(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
    private val paymentMethodMetadataProvider: Provider<PaymentMethodMetadata?>,
    private val workContext: CoroutineContext,
) {
    fun succeeded(
        paymentSelection: PaymentSelection,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intentId: String?,
    ) {
        reportEvent(
            phase = "succeeded",
            event = PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Embedded,
                result = PaymentSheetEvent.Payment.Result.Success,
                duration = durationProvider.end(DurationProvider.Key.Checkout),
                paymentSelection = paymentSelection,
                deferredIntentConfirmationType = deferredIntentConfirmationType,
                intentId = intentId,
            ),
        )
    }

    fun failed(paymentSelection: PaymentSelection, error: PaymentSheetConfirmationError) {
        reportEvent(
            phase = "failed",
            event = PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Embedded,
                result = PaymentSheetEvent.Payment.Result.Failure(error),
                duration = durationProvider.end(DurationProvider.Key.Checkout),
                paymentSelection = paymentSelection,
                deferredIntentConfirmationType = null,
                intentId = null,
            ),
        )
    }

    private fun reportEvent(phase: String, event: PaymentSheetEvent.Payment) {
        val metadata = paymentMethodMetadataProvider.get()
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event = object : AnalyticsEvent {
                        override val eventName = "checkout_elements.confirm.$phase"
                    },
                    additionalParams = metadata.defaultAnalyticsParams() + event.params,
                    publishableKeyOverride = metadata?.apiConfiguration?.publishableKey,
                )
            )
        }
    }
}
