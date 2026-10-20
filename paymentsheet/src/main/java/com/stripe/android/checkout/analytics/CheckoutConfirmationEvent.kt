package com.stripe.android.checkout.analytics

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import kotlin.time.Duration

internal sealed class CheckoutConfirmationEvent : AnalyticsEvent {
    abstract val params: Map<String, Any?>

    class Succeeded(
        context: CheckoutConfirmationContext,
        duration: Duration?,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intentId: String?,
    ) : CheckoutConfirmationEvent() {
        override val eventName: String = "checkout_elements.confirm.succeeded"
        override val params: Map<String, Any?> = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Embedded,
            result = PaymentSheetEvent.Payment.Result.Success,
            duration = duration,
            paymentSelection = context.paymentSelection,
            deferredIntentConfirmationType = deferredIntentConfirmationType,
            intentId = intentId,
        ).params
    }

    class Failed(
        context: CheckoutConfirmationContext,
        duration: Duration?,
        error: PaymentSheetConfirmationError,
    ) : CheckoutConfirmationEvent() {
        override val eventName: String = "checkout_elements.confirm.failed"
        override val params: Map<String, Any?> = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Embedded,
            result = PaymentSheetEvent.Payment.Result.Failure(error),
            duration = duration,
            paymentSelection = context.paymentSelection,
            deferredIntentConfirmationType = null,
            intentId = null,
        ).params
    }
}
