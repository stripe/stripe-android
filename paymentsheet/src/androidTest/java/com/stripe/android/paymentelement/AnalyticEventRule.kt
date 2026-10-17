package com.stripe.android.paymentelement

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
class AnalyticEventRule : TestRule, AnalyticEventCallback {
    private lateinit var events: Turbine<AnalyticEvent>

    override fun onEvent(event: AnalyticEvent) {
        events.add(event)
    }

    suspend fun assertMatchesExpectedEvent(event: AnalyticEvent) {
        assertThat(events.awaitItem()).isEqualTo(event)
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                events = Turbine()
                try {
                    @OptIn(DelicateCoroutinesApi::class)
                    DefaultEventReporter.analyticEventCoroutineContext = newSingleThreadContext("AnalyticEventRule")
                    base.evaluate()
                    events.ensureAllEventsConsumed()
                } finally {
                    DefaultEventReporter.analyticEventCoroutineContext = null
                    events.close()
                }
            }
        }
    }
}
