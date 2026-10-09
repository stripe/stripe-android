package com.stripe.android.polling

import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import javax.inject.Inject
import javax.inject.Provider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PollingAnalyticsEventReporter {
    fun onPollingTimedOut(paymentMethodType: String, lastKnownStatus: String?, timeLimitSeconds: Long)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultPollingAnalyticsEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val apiConfigurationProvider: Provider<ApiConfiguration.State>,
) : PollingAnalyticsEventReporter {

    override fun onPollingTimedOut(paymentMethodType: String, lastKnownStatus: String?, timeLimitSeconds: Long) {
        val event = PollingAnalyticsEvent.TimedOut(paymentMethodType, lastKnownStatus, timeLimitSeconds)
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = event,
                additionalParams = event.params,
                publishableKey = runCatching { apiConfigurationProvider.get().publishableKey }.getOrNull(),
            )
        )
    }
}
