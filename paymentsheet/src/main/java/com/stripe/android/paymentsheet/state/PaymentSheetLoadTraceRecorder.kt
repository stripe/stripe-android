package com.stripe.android.paymentsheet.state

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicReference

public object PaymentSheetLoadTraceRecorder {
    private val currentSession = AtomicReference<Session?>(null)

    public fun startSession() {
        currentSession.set(Session(sessionStartTimeNs = nowNs()))
    }

    public fun finishSession(): TraceResult? {
        val session = currentSession.getAndSet(null) ?: return null
        return session.finish(endTimeNs = nowNs())
    }

    public fun <T> trace(
        name: String,
        block: () -> T,
    ): T {
        val session = currentSession.get()
        if (session == null) {
            return block()
        }

        val startTimeNs = nowNs()
        return try {
            block()
        } finally {
            session.record(
                name = name,
                startTimeNs = startTimeNs,
                endTimeNs = nowNs(),
            )
        }
    }

    public suspend fun <T> traceSuspend(
        name: String,
        block: suspend () -> T,
    ): T {
        val session = currentSession.get()
        if (session == null) {
            return block()
        }

        val startTimeNs = nowNs()
        return try {
            block()
        } finally {
            session.record(
                name = name,
                startTimeNs = startTimeNs,
                endTimeNs = nowNs(),
            )
        }
    }

    private fun nowNs(): Long = SystemClock.elapsedRealtimeNanos()

    private class Session(
        private val sessionStartTimeNs: Long,
    ) {
        private val spans = mutableListOf<TraceSpan>()
        private val lock = Any()

        fun record(
            name: String,
            startTimeNs: Long,
            endTimeNs: Long,
        ) {
            val span = TraceSpan(
                name = name,
                startOffsetMs = (startTimeNs - sessionStartTimeNs).toMs(),
                durationMs = (endTimeNs - startTimeNs).toMs(),
            )
            synchronized(lock) {
                spans += span
            }
        }

        fun finish(endTimeNs: Long): TraceResult {
            val totalDurationMs = (endTimeNs - sessionStartTimeNs).toMs()
            val snapshot = synchronized(lock) {
                spans
                    .sortedWith(
                        compareBy<TraceSpan>({ it.startOffsetMs }, { -it.durationMs }, { it.name })
                    )
                    .toList()
            }
            return TraceResult(
                totalDurationMs = totalDurationMs,
                spans = snapshot,
            )
        }
    }

    public data class TraceResult(
        val totalDurationMs: Double,
        val spans: List<TraceSpan>,
    )

    public data class TraceSpan(
        val name: String,
        val startOffsetMs: Double,
        val durationMs: Double,
    )
}

private fun Long.toMs(): Double = this / 1_000_000.0
