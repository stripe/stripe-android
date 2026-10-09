package com.stripe.android.paymentsheet.utils

import org.junit.runners.model.Statement
import shark.SharkLog

internal enum class LeakCheckReason {
    NoWatchedObjects,
    NoWatchedObjectsAfterIdle,
    NoWatchedObjectsAfterGc,
    NoWatchedObjectsAfterDelayedUiPost,
    NoRetainedObjectsAfterDelay,
}

internal data class LeakCheckDiagnostics(
    val observerStarted: Boolean,
    val noHeapAnalysisReason: LeakCheckReason?,
    val decisionAfterNanos: Long?,
    val assertionDurationMillis: Long?,
)

internal class LeakCheckProfile(private val nanoTime: () -> Long) {
    private var observerStarted = false
    private var observingThread: Thread? = null
    private var previousLogger: SharkLog.Logger? = null
    private var observer: SharkLog.Logger? = null
    private var startedAt = 0L
    private var noHeapAnalysisReason: LeakCheckReason? = null
    private var decisionAfterNanos: Long? = null
    private var assertionDurationMillis: Long? = null

    fun rulesStatement(base: Statement): Statement = object : Statement() {
        override fun evaluate() {
            base.evaluate()
            startObserving()
        }
    }

    fun leakStatement(base: Statement): Statement = object : Statement() {
        override fun evaluate() {
            try {
                base.evaluate()
            } finally {
                if (observerStarted && SharkLog.logger === observer) {
                    SharkLog.logger = previousLogger
                }
                observingThread = null
                previousLogger = null
                observer = null
            }
        }
    }

    fun snapshot(): LeakCheckDiagnostics = LeakCheckDiagnostics(
        observerStarted = observerStarted,
        noHeapAnalysisReason = noHeapAnalysisReason,
        decisionAfterNanos = decisionAfterNanos,
        assertionDurationMillis = assertionDurationMillis,
    )

    private fun startObserving() {
        // Preserve disabled logging, since installing a logger also enables debug message evaluation.
        val logger = SharkLog.logger ?: return
        previousLogger = logger
        startedAt = nanoTime()
        observingThread = Thread.currentThread()
        observer = object : SharkLog.Logger {
            override fun d(message: String) {
                if (Thread.currentThread() === observingThread) {
                    record(message)
                }
                logger.d(message)
            }

            override fun d(throwable: Throwable, message: String) {
                logger.d(throwable, message)
            }
        }
        observerStarted = true
        SharkLog.logger = observer
    }

    private fun record(message: String) {
        val reason = noHeapAnalysisMessages[message]
        if (reason != null && noHeapAnalysisReason == null) {
            noHeapAnalysisReason = reason
            decisionAfterNanos = nanoTime() - startedAt
        }
        if (assertionDurationMillis == null) {
            assertionDurationMillis = assertionDurationMessage.matchEntire(message)
                ?.groupValues?.get(1)?.toLongOrNull()
        }
    }

    private companion object {
        // Export fixed decision codes, never arbitrary log messages or watched-object descriptions.
        val noHeapAnalysisMessages = mapOf(
            "No watched objects." to LeakCheckReason.NoWatchedObjects,
            "No watched objects after waiting for idle sync." to LeakCheckReason.NoWatchedObjectsAfterIdle,
            "No watched objects after triggering an explicit GC." to LeakCheckReason.NoWatchedObjectsAfterGc,
            "No watched objects after delayed UI post is cleared." to
                LeakCheckReason.NoWatchedObjectsAfterDelayedUiPost,
            "No retained objects after waiting for retained delay." to LeakCheckReason.NoRetainedObjectsAfterDelay,
        ).mapKeys { (message, _) -> "Test can keep going: no heap dump performed ($message)" }

        val assertionDurationMessage = Regex(
            "Spent ([0-9]+) ms detecting leaks on DetectLeaksAfterTestSuccess, VM total so far: [0-9]+ ms"
        )
    }
}
