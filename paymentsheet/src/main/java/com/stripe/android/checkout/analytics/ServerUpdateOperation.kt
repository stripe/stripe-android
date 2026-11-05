package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Reports a merchant server update and the session refresh that follows it. */
internal class ServerUpdateOperation(
    report: (CheckoutAnalyticsEvent) -> Unit,
) : CheckoutAnalyticsOperation<Throwable>(Operation.RunServerUpdate, report) {
    private var startedAt: TimeMark? = null

    private var merchantUpdateDuration: Duration? = null
    private var refreshDuration: Duration? = null

    suspend fun <T> measureMerchantUpdate(block: suspend () -> T): T {
        val start = TimeSource.Monotonic.markNow()
        try {
            return block()
        } finally {
            merchantUpdateDuration = start.elapsedNow()
        }
    }

    suspend fun <T> measureRefresh(block: suspend () -> T): T {
        val start = TimeSource.Monotonic.markNow()
        try {
            return block()
        } finally {
            refreshDuration = start.elapsedNow()
        }
    }

    override val params: Map<String, Any?>
        get() = buildMap {
            merchantUpdateDuration?.let { put("merchant_update_duration", it.inSeconds()) }
            refreshDuration?.let { put("refresh_duration", it.inSeconds()) }
        }

    override fun started(response: CheckoutSessionResponse?) {
        startedAt = TimeSource.Monotonic.markNow()
        super.started(response)
    }

    override fun elapsed(): Duration? = startedAt?.elapsedNow()

    override fun analyticsError(error: Throwable): CheckoutAnalyticsEvent.Error =
        CheckoutAnalyticsEvent.Error.from(error)

    private fun Duration.inSeconds(): Float = toDouble(DurationUnit.SECONDS).toFloat()
}
