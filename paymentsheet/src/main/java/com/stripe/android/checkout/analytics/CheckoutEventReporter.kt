package com.stripe.android.checkout.analytics

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@Singleton
internal class CheckoutEventReporter internal constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val timeSource: TimeSource,
) {
    @Inject
    constructor(
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        analyticsRequestFactory: AnalyticsRequestFactory,
    ) : this(
        analyticsRequestExecutor = analyticsRequestExecutor,
        analyticsRequestFactory = analyticsRequestFactory,
        timeSource = TimeSource.Monotonic,
    )

    fun start(
        operation: CheckoutAnalyticsOperation,
        response: CheckoutSessionResponse?,
        additionalParams: Map<String, Any?>,
    ): Span {
        val startedAt = timeSource.markNow()
        val initialParams = snapshotParams(response, additionalParams)
            .filterKeys { it != FIELD_DURATION }
        val span = Span(
            operation = operation,
            initialParams = initialParams,
            startedAt = startedAt,
        )
        span.reportStarted()
        return span
    }

    fun resume(
        operation: CheckoutAnalyticsOperation,
        response: CheckoutSessionResponse?,
        additionalParams: Map<String, Any?>,
    ): Span = Span(
        operation = operation,
        initialParams = snapshotParams(response, additionalParams)
            .filterKeys { it != FIELD_DURATION },
        startedAt = null,
    )

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
                if (value != null) put(key, snapshotValue(value))
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
                additionalParams = additionalParams.filterValues { it != null } +
                    (FIELD_PRODUCT_USAGE to PRODUCT_USAGE_CHECKOUT_ELEMENTS),
            )
        )
    }

    private fun terminalParams(
        currentResponse: CheckoutSessionResponse?,
        additionalParams: Map<String, Any?>,
        requestId: String?,
        recordedRequestId: String?,
        error: Throwable?,
        duration: Duration?,
        measuredDurations: Map<String, Float>,
    ): Map<String, Any> = buildMap {
        putAll(snapshotParams(currentResponse, emptyMap()))
        putAll(snapshotParams(response = null, additionalParams = additionalParams))
        putAll(errorParams(error))
        (requestId ?: (error as? StripeException)?.requestId ?: recordedRequestId)
            ?.let { put(FIELD_REQUEST_ID, it) }
        measuredDurations.forEach { (key, value) -> put(key, value) }
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

    internal inner class Span internal constructor(
        private val operation: CheckoutAnalyticsOperation,
        private val initialParams: Map<String, Any>,
        private val startedAt: TimeMark?,
    ) {
        private val lock = Any()
        private val addedParams = mutableMapOf<String, Any>()
        private val measuredDurations = mutableMapOf<String, Float>()
        private var terminalReported = false
        private var recordedRequestId: String? = null

        internal fun recordRequestId(requestId: String?) {
            synchronized(lock) {
                if (!terminalReported) {
                    recordedRequestId = requestId
                }
            }
        }

        internal fun addParams(additionalParams: Map<String, Any?>) {
            synchronized(lock) {
                if (!terminalReported) {
                    addedParams.putAll(
                        snapshotParams(response = null, additionalParams = additionalParams)
                    )
                }
            }
        }

        internal suspend fun <T> measure(key: String, block: suspend () -> T): T {
            val measureStartedAt = timeSource.markNow()
            return try {
                block()
            } finally {
                synchronized(lock) {
                    if (!terminalReported) {
                        measuredDurations[key] = measureStartedAt.elapsedNow()
                            .toDouble(DurationUnit.SECONDS)
                            .toFloat()
                    }
                }
            }
        }

        internal fun succeeded(
            response: CheckoutSessionResponse?,
            requestId: String?,
            additionalParams: Map<String, Any?>,
        ) {
            reportTerminal(
                phase = PHASE_SUCCEEDED,
                response = response,
                additionalParams = additionalParams,
                requestId = requestId,
                error = null,
            )
        }

        internal fun failed(
            error: Throwable,
            response: CheckoutSessionResponse?,
            additionalParams: Map<String, Any?>,
        ) {
            reportTerminal(
                phase = PHASE_FAILED,
                response = response,
                additionalParams = additionalParams,
                requestId = null,
                error = error,
            )
        }

        internal fun canceled(
            response: CheckoutSessionResponse?,
            additionalParams: Map<String, Any?>,
        ) {
            reportTerminal(
                phase = PHASE_CANCELED,
                response = response,
                additionalParams = additionalParams,
                requestId = null,
                error = null,
            )
        }

        internal fun reportStarted() {
            reportEvent(
                eventName = eventName(PHASE_STARTED),
                additionalParams = initialParams,
            )
        }

        private fun reportTerminal(
            phase: String,
            response: CheckoutSessionResponse?,
            additionalParams: Map<String, Any?>,
            requestId: String?,
            error: Throwable?,
        ) {
            val snapshot = synchronized(lock) {
                if (terminalReported) {
                    null
                } else {
                    terminalReported = true
                    SpanTerminalSnapshot(
                        addedParams = addedParams.toMap(),
                        measuredDurations = measuredDurations.toMap(),
                        recordedRequestId = recordedRequestId,
                        duration = startedAt?.elapsedNow(),
                    )
                }
            } ?: return

            reportEvent(
                eventName = eventName(phase),
                additionalParams = buildMap {
                    putAll(initialParams)
                    putAll(snapshot.addedParams)
                    putAll(
                        terminalParams(
                            currentResponse = response,
                            additionalParams = additionalParams,
                            requestId = requestId,
                            recordedRequestId = snapshot.recordedRequestId,
                            error = error,
                            duration = snapshot.duration,
                            measuredDurations = snapshot.measuredDurations,
                        )
                    )
                },
            )
        }

        private fun eventName(phase: String): String =
            "$EVENT_NAMESPACE.${operation.value}.$phase"
    }

    private companion object {
        const val EVENT_NAMESPACE = "checkout_elements"
        const val EVENT_CLEAR_PAYMENT_OPTION = "$EVENT_NAMESPACE.clear_payment_option"

        const val FIELD_DURATION = "duration"
        const val FIELD_ERROR_TYPE = "error_type"
        const val FIELD_ERROR_CODE = "error_code"
        const val FIELD_PRODUCT_USAGE = "product_usage"
        const val FIELD_REQUEST_ID = "request_id"

        const val PRODUCT_USAGE_CHECKOUT_ELEMENTS = "checkout_elements"
        const val VALUE_UNKNOWN_ERROR_TYPE = "unknown"

        const val PHASE_STARTED = "started"
        const val PHASE_SUCCEEDED = "succeeded"
        const val PHASE_FAILED = "failed"
        const val PHASE_CANCELED = "canceled"
    }

    private data class SpanTerminalSnapshot(
        val addedParams: Map<String, Any>,
        val measuredDurations: Map<String, Float>,
        val recordedRequestId: String?,
        val duration: Duration?,
    )
}
