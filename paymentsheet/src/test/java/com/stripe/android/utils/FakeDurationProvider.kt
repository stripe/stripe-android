package com.stripe.android.utils

import com.stripe.android.core.utils.DurationProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FakeDurationProvider(
    private val duration: Duration = 1.seconds,
) : DurationProvider {
    private val calls: MutableList<Call> = mutableListOf()
    private val completedDurations: MutableMap<DurationProvider.Key, Duration> = mutableMapOf()

    override fun start(key: DurationProvider.Key, reset: Boolean) {
        calls.add(Call.Start(key, reset))
        if (reset) {
            completedDurations.remove(key)
        }
    }

    override fun elapsed(key: DurationProvider.Key): Duration {
        calls.add(Call.Elapsed(key))
        return duration
    }

    override fun end(key: DurationProvider.Key): Duration {
        calls.add(Call.End(key))
        completedDurations[key] = duration
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

    fun has(call: Call): Boolean = calls.contains(call)

    sealed interface Call {
        val key: DurationProvider.Key

        data class Start(override val key: DurationProvider.Key, val reset: Boolean) : Call

        data class Elapsed(override val key: DurationProvider.Key) : Call

        data class End(override val key: DurationProvider.Key) : Call
    }
}
