package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.core.networking.AnalyticsRequestV2
import com.stripe.android.core.networking.AnalyticsRequestV2Executor
import javax.inject.Inject

internal class FakeAnalyticsRequestV2Executor @Inject constructor() : AnalyticsRequestV2Executor {
    private val _enqueueCalls = Turbine<AnalyticsRequestV2>()
    val enqueueCalls: ReceiveTurbine<AnalyticsRequestV2> = _enqueueCalls

    override suspend fun enqueue(request: AnalyticsRequestV2) {
        _enqueueCalls.add(request)
    }

    fun validate() {
        _enqueueCalls.ensureAllEventsConsumed()
    }
}
