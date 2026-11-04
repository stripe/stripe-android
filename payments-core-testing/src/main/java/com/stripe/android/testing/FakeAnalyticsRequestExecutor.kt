package com.stripe.android.testing

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import org.junit.Assert.assertEquals

class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
    private val _requestTurbine = Turbine<AnalyticsRequest>()
    val requestTurbine: ReceiveTurbine<AnalyticsRequest> = _requestTurbine

    override fun executeAsync(request: AnalyticsRequest) {
        _requestTurbine.add(request)
    }

    suspend fun awaitEvent(eventName: String): Map<String, *> {
        val params = requestTurbine.awaitItem().params
        assertEquals(eventName, params["event"])
        return params
    }

    fun validate() {
        _requestTurbine.ensureAllEventsConsumed()
    }
}
