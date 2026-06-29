package com.stripe.android.polling

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultPollingAnalyticsEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
) : PollingAnalyticsEventReporter {

    override fun onPollingTimedOut(paymentMethodType: String, lastKnownStatus: String?) {
        val event = PollingAnalyticsEvent.TimedOut(paymentMethodType, lastKnownStatus)
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = event,
                additionalParams = event.params,
            )
        )
    }
}
