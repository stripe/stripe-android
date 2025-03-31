package com.stripe.android.paymentelement

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
class AnalyticMatcher : AnalyticEventCallback {
    private val _events = Turbine<AnalyticEvent>()
    private val events: ReceiveTurbine<AnalyticEvent> = _events
    override fun onEvent(event: AnalyticEvent) {
        _events.add(event)
    }
    suspend fun validateAnalyticEvent(event: AnalyticEvent) {
        assertThat(events.awaitItem()).isEqualTo(event)
    }
}
