package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor

internal class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
    private val _requestTurbine = Turbine<AnalyticsRequest>()
    val requestTurbine: ReceiveTurbine<AnalyticsRequest> = _requestTurbine

    override fun executeAsync(request: AnalyticsRequest) {
        _requestTurbine.add(request)
    }

    suspend fun awaitEvent(eventName: String): Map<String, *> {
        val params = requestTurbine.awaitItem().params
        assertThat(params["event"]).isEqualTo(eventName)
        return params
    }

    fun validate() {
        _requestTurbine.ensureAllEventsConsumed()
    }
}
