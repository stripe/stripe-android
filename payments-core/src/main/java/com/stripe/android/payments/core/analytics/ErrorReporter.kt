package com.stripe.android.payments.core.analytics

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsEvent

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ErrorReporter {

    fun report(errorEvent: ErrorEvent, stripeException: StripeException)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class ErrorEvent(override val eventName: String) : AnalyticsEvent {
        GET_SAVED_PAYMENT_METHODS_FAILURE(
            eventName = "elements.customer_repository.get_saved_payment_methods_failure"
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        fun getAdditionalParamsFromError(error: Throwable): Map<String, String> {
            return when (error) {
                is StripeException -> getAdditionalParamsFromStripeException(error)
                else -> getAdditionalParamsFromStripeException(StripeException.create(error))
            }
        }
        fun getAdditionalParamsFromStripeException(stripeException: StripeException): Map<String, String> {
            return mapOf(
                "analytics_value" to stripeException.analyticsValue(),
                "status_code" to stripeException.statusCode.toString(),
                "request_id" to stripeException.requestId,
                "error_type" to stripeException.stripeError?.type,
                "error_code" to stripeException.stripeError?.code,
            ).filterNotNullValues()
        }

        private fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
            mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
    }
}
