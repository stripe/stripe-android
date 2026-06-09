package com.stripe.android.testing

import app.cash.turbine.Turbine
import com.stripe.android.polling.PollingAnalyticsEventReporter

class FakePollingAnalyticsEventReporter : PollingAnalyticsEventReporter {
    private val calls = Turbine<Call>()

    override fun onPollingTimedOut(paymentMethodType: String, lastKnownStatus: String?) {
        calls.add(Call.PollingTimedOut(paymentMethodType, lastKnownStatus))
    }

    suspend fun awaitCall(): Call {
        return calls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    sealed interface Call {
        data class PollingTimedOut(
            val paymentMethodType: String,
            val lastKnownStatus: String?,
        ) : Call
    }
}
