package com.stripe.android.analytics

import com.stripe.android.core.utils.DurationProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FakeDurationProvider(
    private val duration: Duration = 1.seconds,
) : DurationProvider {
    private val calls: MutableList<Call> = mutableListOf()

    override fun start(key: DurationProvider.Key, reset: Boolean) {
        calls.add(Call.Start(key, reset))
    }

    override fun elapsed(key: DurationProvider.Key): Duration {
        calls.add(Call.Elapsed(key))
        return duration
    }

    override fun end(key: DurationProvider.Key): Duration {
        calls.add(Call.End(key))
        return duration
    }

    override fun completedDuration(key: DurationProvider.Key): Duration? {
        throw NotImplementedError("completedDuration is not implemented in FakeDurationProvider")
    }

    override suspend fun <T> measureDuration(key: DurationProvider.Key, block: suspend () -> T): T {
        calls.add(Call.Measure(key))
        return block()
    }

    fun has(call: Call): Boolean = calls.contains(call)

    sealed interface Call {
        val key: DurationProvider.Key

        data class Start(override val key: DurationProvider.Key, val reset: Boolean) : Call

        data class Elapsed(override val key: DurationProvider.Key) : Call

        data class End(override val key: DurationProvider.Key) : Call

        data class Measure(override val key: DurationProvider.Key) : Call
    }
}
