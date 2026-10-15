package com.stripe.android.checkout.analytics

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class CheckoutEventReporter internal constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val timeSource: TimeSource,
) {
    @Inject
    constructor(
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        @CheckoutAnalytics analyticsRequestFactory: AnalyticsRequestFactory,
    ) : this(analyticsRequestExecutor, analyticsRequestFactory, TimeSource.Monotonic)

    fun started(params: Map<String, Any>): TimeMark {
        val startedAt = timeSource.markNow()
        reportEvent("started", params)
        return startedAt
    }

    fun succeeded(params: Map<String, Any>, requestId: String?, duration: Duration?) {
        reportEvent("succeeded", params + terminalParams(requestId, duration))
    }

    fun failed(params: Map<String, Any>, error: Throwable, duration: Duration?) {
        val stripeException = error as? StripeException
        val errorParams = buildMap {
            put("error_type", stripeException?.stripeError?.type ?: error.javaClass.simpleName)
            stripeException?.stripeError?.code?.let { put("error_code", it) }
        }
        reportEvent("failed", params + errorParams + terminalParams(stripeException?.requestId, duration))
    }

    fun canceled(params: Map<String, Any>, duration: Duration?) {
        reportEvent("canceled", params + terminalParams(requestId = null, duration = duration))
    }

    private fun terminalParams(requestId: String?, duration: Duration?): Map<String, Any> = buildMap {
        requestId?.let { put("request_id", it) }
        duration?.let { put("duration", it.toDouble(DurationUnit.SECONDS).toFloat()) }
    }

    private fun reportEvent(phase: String, params: Map<String, Any>) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = object : AnalyticsEvent {
                    override val eventName = "checkout_elements.confirm.$phase"
                },
                additionalParams = params,
            )
        )
    }
}
