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
        PHONE_NUMBER_FORMATTER_NULL_LOCALE(partialEventName = "elements.phone_number_formatter.user_locale_null");

        override val eventName: String
            get() = "unexpected_error.$partialEventName"
    }
}
