package com.stripe.android.paymentmethodmessaging.element.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor

internal class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
    private val _requestTurbine = Turbine<AnalyticsRequest>()
    val requestTurbine: ReceiveTurbine<AnalyticsRequest> = _requestTurbine

    override fun executeAsync(request: AnalyticsRequest) {
        _requestTurbine.add(request)
    }

    fun validate() {
        _requestTurbine.ensureAllEventsConsumed()
    }
}
