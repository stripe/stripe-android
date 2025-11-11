package com.stripe.android.paymentmethodmessaging.element.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.core.utils.DurationProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FakeDurationProvider(
    private val duration: Duration = 1.seconds,
) : DurationProvider {
    private val _callsTurbine: Turbine<Call> = Turbine<Call>()
    val callsTurbine: ReceiveTurbine<Call> = _callsTurbine

    override fun start(key: DurationProvider.Key, reset: Boolean) {
        _callsTurbine.add(Call.Start(key, reset))
    }

    override fun end(key: DurationProvider.Key): Duration {
        _callsTurbine.add(Call.End(key))
        return duration
    }

    fun validate() {
        _callsTurbine.ensureAllEventsConsumed()
    }

    sealed interface Call {
        val key: DurationProvider.Key

        data class Start(override val key: DurationProvider.Key, val reset: Boolean) : Call

        data class End(override val key: DurationProvider.Key) : Call
    }
}
