package com.stripe.android.paymentelement

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
class AnalyticEventRule : TestRule, AnalyticEventCallback {
    private var _events = Turbine<AnalyticEvent>()
    private val events: ReceiveTurbine<AnalyticEvent> = _events

    override fun onEvent(event: AnalyticEvent) {
        _events.add(event)
    }

    suspend fun validateAnalyticEvent(event: AnalyticEvent) {
        assertThat(events.awaitItem()).isEqualTo(event)
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    base?.evaluate()
                    _events.expectNoEvents()
                } finally {
                    _events.close()
                    _events = Turbine()
                }
            }
        }
    }
}
