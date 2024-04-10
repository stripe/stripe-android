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

    override fun end(key: DurationProvider.Key): Duration {
        calls.add(Call.End(key))
        return duration
    }

    fun has(call: Call): Boolean = calls.contains(call)

    sealed interface Call {
        val key: DurationProvider.Key

        data class Start(override val key: DurationProvider.Key, val reset: Boolean) : Call

        data class End(override val key: DurationProvider.Key) : Call
    }
}
