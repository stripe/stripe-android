package com.stripe.android.payments.core.analytics

import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import javax.inject.Inject
import javax.inject.Provider

/**
 * [ErrorReporter] which sends error analytics via [AnalyticsRequestExecutor].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RealErrorReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val apiConfigurationProvider: Provider<ApiConfiguration.State>,
) : ErrorReporter {
    override fun report(
        errorEvent: ErrorReporter.ErrorEvent,
        stripeException: StripeException?,
        additionalNonPiiParams: Map<String, String>,
        publishableKeyOverride: String?
    ) {
        val paramsFromStripeException = if (stripeException == null) {
            emptyMap()
        } else {
            ErrorReporter.getAdditionalParamsFromStripeException(stripeException = stripeException)
        }
        val additionalParams = paramsFromStripeException + additionalNonPiiParams
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = errorEvent,
                additionalParams = additionalParams,
                publishableKey = publishableKeyOverride
                    ?: runCatching { apiConfigurationProvider.get().publishableKey }.getOrNull(),
            )
        )
    }
}
