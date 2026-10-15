package com.stripe.android.checkout.analytics

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
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
    ) : this(
        analyticsRequestExecutor = analyticsRequestExecutor,
        analyticsRequestFactory = analyticsRequestFactory,
        timeSource = TimeSource.Monotonic,
    )

    fun markNow(): TimeMark = timeSource.markNow()

    fun started(
        operation: CheckoutAnalyticsOperation,
        response: CheckoutSessionResponse?,
        additionalParams: Map<String, Any?>,
    ): TimeMark {
        val startedAt = markNow()
        reportEvent(
            eventName = eventName(operation, PHASE_STARTED),
            additionalParams = snapshotParams(response, additionalParams)
                .filterKeys { it != FIELD_DURATION },
        )
        return startedAt
    }

    fun succeeded(
        operation: CheckoutAnalyticsOperation,
        response: CheckoutSessionResponse?,
        requestId: String?,
        duration: Duration?,
        additionalParams: Map<String, Any?>,
    ) {
        reportEvent(
            eventName = eventName(operation, PHASE_SUCCEEDED),
            additionalParams = terminalParams(
                response = response,
                additionalParams = additionalParams,
                requestId = requestId,
                error = null,
                duration = duration,
            ),
        )
    }

    fun failed(
        operation: CheckoutAnalyticsOperation,
        error: Throwable,
        response: CheckoutSessionResponse?,
        requestId: String?,
        duration: Duration?,
        additionalParams: Map<String, Any?>,
    ) {
        reportEvent(
            eventName = eventName(operation, PHASE_FAILED),
            additionalParams = terminalParams(
                response = response,
                additionalParams = additionalParams,
                requestId = requestId,
                error = error,
                duration = duration,
            ),
        )
    }

    fun canceled(
        operation: CheckoutAnalyticsOperation,
        response: CheckoutSessionResponse?,
        duration: Duration?,
        additionalParams: Map<String, Any?>,
    ) {
        reportEvent(
            eventName = eventName(operation, PHASE_CANCELED),
            additionalParams = terminalParams(
                response = response,
                additionalParams = additionalParams,
                requestId = null,
                error = null,
                duration = duration,
            ),
        )
    }

    fun reportClearPaymentOption(response: CheckoutSessionResponse?) {
        reportEvent(
            eventName = EVENT_CLEAR_PAYMENT_OPTION,
            additionalParams = snapshotParams(response, emptyMap()),
        )
    }

    private fun snapshotParams(
        response: CheckoutSessionResponse?,
        additionalParams: Map<String, Any?>,
    ): Map<String, Any> = buildMap {
        response?.checkoutAnalyticsParams()
            ?.filterKeys { it != FIELD_REQUEST_ID }
            ?.forEach { (key, value) ->
                put(key, snapshotValue(value))
            }
        additionalParams.forEach { (key, value) ->
            if (value != null && key != FIELD_REQUEST_ID) put(key, snapshotValue(value))
        }
    }

    private fun reportEvent(
        eventName: String,
        additionalParams: Map<String, Any?>,
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = object : AnalyticsEvent {
                    override val eventName: String = eventName
                },
                additionalParams = additionalParams.filterValues { it != null },
            )
        )
    }

    private fun terminalParams(
        response: CheckoutSessionResponse?,
        additionalParams: Map<String, Any?>,
        requestId: String?,
        error: Throwable?,
        duration: Duration?,
    ): Map<String, Any> = buildMap {
        putAll(snapshotParams(response, emptyMap()))
        putAll(
            snapshotParams(response = null, additionalParams = additionalParams)
                .filterKeys { it != FIELD_DURATION }
        )
        putAll(errorParams(error))
        ((error as? StripeException)?.requestId ?: requestId)
            ?.let { put(FIELD_REQUEST_ID, it) }
        duration?.let {
            put(FIELD_DURATION, it.toDouble(DurationUnit.SECONDS).toFloat())
        }
    }

    private fun errorParams(error: Throwable?): Map<String, Any> = when (error) {
        null -> emptyMap()
        is StripeException -> buildMap {
            put(
                FIELD_ERROR_TYPE,
                error.stripeError?.type ?: error.javaClass.simpleName.ifBlank {
                    VALUE_UNKNOWN_ERROR_TYPE
                },
            )
            error.stripeError?.code?.let { put(FIELD_ERROR_CODE, it) }
        }
        else -> mapOf(
            FIELD_ERROR_TYPE to error.javaClass.simpleName.ifBlank { VALUE_UNKNOWN_ERROR_TYPE }
        )
    }

    private fun snapshotValue(value: Any): Any = when (value) {
        is Map<*, *> -> value.entries.associateTo(linkedMapOf()) { (key, nestedValue) ->
            key?.let(::snapshotValue) to nestedValue?.let(::snapshotValue)
        }
        is List<*> -> value.map { nestedValue -> nestedValue?.let(::snapshotValue) }
        is Set<*> -> value.mapTo(linkedSetOf()) { nestedValue -> nestedValue?.let(::snapshotValue) }
        is Array<*> -> value.map { nestedValue -> nestedValue?.let(::snapshotValue) }
        else -> value
    }

    private fun eventName(
        operation: CheckoutAnalyticsOperation,
        phase: String,
    ): String = EVENT_NAMESPACE + "." + operation.value + "." + phase

    private companion object {
        const val EVENT_NAMESPACE = "checkout_elements"
        const val EVENT_CLEAR_PAYMENT_OPTION = "checkout_elements.clear_payment_option"

        const val FIELD_DURATION = "duration"
        const val FIELD_ERROR_TYPE = "error_type"
        const val FIELD_ERROR_CODE = "error_code"
        const val FIELD_REQUEST_ID = "request_id"

        const val VALUE_UNKNOWN_ERROR_TYPE = "unknown"

        const val PHASE_STARTED = "started"
        const val PHASE_SUCCEEDED = "succeeded"
        const val PHASE_FAILED = "failed"
        const val PHASE_CANCELED = "canceled"
    }
}
