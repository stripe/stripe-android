package com.stripe.android.payments.core.analytics

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import javax.inject.Inject

/**
 * [ErrorReporter] which sends error analytics via [AnalyticsRequestExecutor].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RealErrorReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory
) : ErrorReporter {
    override fun report(errorEvent: ErrorReporter.ErrorEvent, stripeException: StripeException) {
        val additionalParams = mapOf(
            "analyticsValue" to stripeException.analyticsValue(),
            "statusCode" to stripeException.statusCode.toString(),
        ).filterNotNullValues()
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(errorEvent, additionalParams)
        )
    }

    private fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
        mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
}
