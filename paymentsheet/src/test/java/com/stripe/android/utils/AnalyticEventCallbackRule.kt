package com.stripe.android.utils

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import javax.inject.Provider

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
class AnalyticEventCallbackRule : TestWatcher(), AnalyticEventCallback, Provider<AnalyticEventCallback?> {
    private lateinit var analyticEventCall: Turbine<AnalyticEvent>
    private var callback: AnalyticEventCallback? = null

    override fun get(): AnalyticEventCallback? = callback
    override fun onEvent(event: AnalyticEvent) {
        analyticEventCall.add(event)
    }

    fun setCallback(callback: AnalyticEventCallback?) {
        this.callback = callback
    }

    suspend fun assertMatchesExpectedEvent(event: AnalyticEvent) {
        assertThat(analyticEventCall.awaitItem()).isEqualTo(event)
    }

    override fun starting(description: Description) {
        super.starting(description)
        analyticEventCall = Turbine()
        setCallback(this)
    }

    override fun finished(description: Description) {
        try {
            analyticEventCall.ensureAllEventsConsumed()
        } finally {
            setCallback(null)
            analyticEventCall.close()
        }
        super.finished(description)
    }
}
