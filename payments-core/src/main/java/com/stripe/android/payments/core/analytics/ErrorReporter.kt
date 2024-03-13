package com.stripe.android.payments.core.analytics

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsEvent

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ErrorReporter {

    fun report(errorEvent: ErrorEvent, stripeException : StripeException)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class ErrorEvent(override val eventName: String) : AnalyticsEvent {
        GET_SAVED_PAYMENT_METHODS_FAILURE(
            eventName = "elements.customer_repository.get_saved_payment_methods_failure"
        )
    }
}
