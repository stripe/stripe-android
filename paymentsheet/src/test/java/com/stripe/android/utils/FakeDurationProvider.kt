package com.stripe.android.utils

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.core.utils.DurationProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FakeDurationProvider(
    // Null models a provider that never saw the start, e.g. after process death.
    private val duration: Duration? = 1.seconds,
) : DurationProvider {
    private val _callsTurbine: Turbine<Call> = Turbine<Call>()
    val callsTurbine: ReceiveTurbine<Call> = _callsTurbine

    private val completedDurations: MutableMap<DurationProvider.Key, Duration> = mutableMapOf()

    override fun start(key: DurationProvider.Key, reset: Boolean) {
        _callsTurbine.add(Call.Start(key, reset))
        if (reset) {
            completedDurations.remove(key)
        }
    }

    override fun elapsed(key: DurationProvider.Key): Duration? {
        _callsTurbine.add(Call.Elapsed(key))
        return duration
    }

    override fun end(key: DurationProvider.Key): Duration? {
        _callsTurbine.add(Call.End(key))
        duration?.let { completedDurations[key] = it }
        return duration
    }

    override fun completedDuration(key: DurationProvider.Key): Duration? {
        return completedDurations[key]
    }

    override suspend fun <T> measureDuration(
        key: DurationProvider.Key,
        block: suspend () -> T,
    ): T {
        start(key, reset = true)
        return try {
            block()
        } finally {
            end(key)
        }
    }

    fun validate() {
        _callsTurbine.ensureAllEventsConsumed()
    }

    sealed interface Call {
        val key: DurationProvider.Key

        data class Start(override val key: DurationProvider.Key, val reset: Boolean) : Call

        data class Elapsed(override val key: DurationProvider.Key) : Call

        data class End(override val key: DurationProvider.Key) : Call
    }
}
