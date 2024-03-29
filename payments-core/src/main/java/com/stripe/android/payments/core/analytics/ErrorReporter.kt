package com.stripe.android.payments.core.analytics

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsEvent

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ErrorReporter {

    fun report(errorEvent: ErrorEvent, stripeException: StripeException)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface ErrorEvent : AnalyticsEvent

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class ExpectedErrorEvent(override val eventName: String) : ErrorEvent {
        GET_SAVED_PAYMENT_METHODS_FAILURE(
            eventName = "elements.customer_repository.get_saved_payment_methods_failure"
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class UnexpectedErrorEvent(val partialEventName: String) : ErrorEvent {
        PAYMENT_AND_SETUP_INTENT_MISSING_US_BANK_ACTIVITY(
            partialEventName = "elements.manual_us_bank_activity.payment_and_setup_intent_missing"
        );

        override val eventName: String
            get() = "unexpected_error.$partialEventName"
    }
}
