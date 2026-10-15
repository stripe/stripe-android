package com.stripe.android.paymentsheet.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.IdentityHashMap
import org.junit.runners.model.Statement

internal enum class ActivityCollectionType {
    MainActivity,
    PaymentSheetActivity,
    PaymentOptionsActivity,
    Other,
}

internal data class ActivityCollectionObservation(
    val type: ActivityCollectionType,
    val destroyedAfterNanos: Long,
    val collectionObservedAfterNanos: Long?,
)

internal data class ActivityCollectionDiagnostics(
    val observerStarted: Boolean,
    val observerCompleted: Boolean,
    val workerExitedUnexpectedly: Boolean,
    val leakCheckStartedAfterNanos: Long?,
    val leakCheckFinishedAfterNanos: Long?,
    val finishedAfterNanos: Long?,
    val activities: List<ActivityCollectionObservation>,
)

internal class ActivityCollectionProfile internal constructor(
    private val application: Application,
    private val nanoTime: () -> Long,
    private val referenceFactory: (Activity, ReferenceQueue<Activity>) -> WeakReference<Activity>,
) {
    constructor(
        application: Application,
        nanoTime: () -> Long,
    ) : this(
        application = application,
        nanoTime = nanoTime,
        referenceFactory = { activity, queue -> WeakReference(activity, queue) },
    )

    private val lock = Any()
    private val referenceQueue = ReferenceQueue<Activity>()
    private val activityObservations = mutableListOf<MutableActivityCollectionObservation>()
    private val observationsByReference =
        IdentityHashMap<Reference<out Activity>, MutableActivityCollectionObservation>()

    @Volatile
    private var startedAtNanos: Long? = null

    @Volatile
    private var observerStarted = false

    @Volatile
    private var observerCompleted = false

    @Volatile
    private var workerExitedUnexpectedly = false

    @Volatile
    private var leakCheckStartedAfterNanos: Long? = null

    @Volatile
    private var leakCheckFinishedAfterNanos: Long? = null

    @Volatile
    private var finishedAfterNanos: Long? = null

    @Volatile
    private var workerShutdownRequested = false

    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    private var callbacksRegistered = false
    private var worker: Thread? = null

    fun rulesStatement(base: Statement): Statement =
        object : Statement() {
            override fun evaluate() {
                startObserving()
                base.evaluate()
                leakCheckStartedAfterNanos = elapsedSinceStart()
            }
        }

    fun statement(base: Statement): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } finally {
                    finishObserving()
                }
            }
        }

    fun snapshot(): ActivityCollectionDiagnostics {
        val activities = synchronized(lock) {
            activityObservations.map { observation ->
                ActivityCollectionObservation(
                    type = observation.type,
                    destroyedAfterNanos = observation.destroyedAfterNanos,
                    collectionObservedAfterNanos = observation.collectionObservedAfterNanos,
                )
            }
        }
        return ActivityCollectionDiagnostics(
            observerStarted = observerStarted,
            observerCompleted = observerCompleted,
            workerExitedUnexpectedly = workerExitedUnexpectedly,
            leakCheckStartedAfterNanos = leakCheckStartedAfterNanos,
            leakCheckFinishedAfterNanos = leakCheckFinishedAfterNanos,
            finishedAfterNanos = finishedAfterNanos,
            activities = activities,
        )
    }

    private fun startObserving() {
        if (startedAtNanos != null) return
        startedAtNanos = nanoTime()

        val callbacks = createLifecycleCallbacks()
        lifecycleCallbacks = callbacks
        application.registerActivityLifecycleCallbacks(callbacks)
        callbacksRegistered = true

        val worker = Thread(::observeReferenceQueue, "ActivityCollectionProfile").apply {
            isDaemon = true
        }
        this.worker = worker
        worker.start()
        observerStarted = true
    }

    private fun createLifecycleCallbacks() = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityResumed(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) {
            val type = activityType(activity)
            val observation = MutableActivityCollectionObservation(
                type = type,
                destroyedAfterNanos = elapsedSinceStart(),
                collectionObservedAfterNanos = null,
            )
            synchronized(lock) {
                val reference = referenceFactory(activity, referenceQueue)
                activityObservations.add(observation)
                observationsByReference[reference] = observation
            }
        }
    }

    private fun observeReferenceQueue() {
        try {
            while (true) {
                val reference = referenceQueue.remove()
                recordCollectionObservation(reference)
            }
        } catch (interrupted: InterruptedException) {
            if (!workerShutdownRequested) {
                workerExitedUnexpectedly = true
            }
        } catch (_: Throwable) {
            workerExitedUnexpectedly = true
        }
    }

    private fun recordCollectionObservation(reference: Reference<out Activity>) {
        synchronized(lock) {
            observationsByReference.remove(reference)?.let { observation ->
                observation.collectionObservedAfterNanos = elapsedSinceStart()
            }
        }
    }

    private fun drainQueuedReferences() {
        while (true) {
            val reference = referenceQueue.poll() ?: return
            recordCollectionObservation(reference)
        }
    }

    private fun finishObserving() {
        val startedAt = startedAtNanos
        if (startedAt != null && leakCheckStartedAfterNanos != null) {
            leakCheckFinishedAfterNanos = elapsedSinceStart()
        }

        var cleanupSucceeded = observerStarted
        val callbacks = lifecycleCallbacks
        if (callbacksRegistered && callbacks != null) {
            try {
                application.unregisterActivityLifecycleCallbacks(callbacks)
                callbacksRegistered = false
            } catch (_: RuntimeException) {
                cleanupSucceeded = false
            }
        }

        val worker = worker
        if (worker == null) {
            cleanupSucceeded = false
        } else {
            workerShutdownRequested = true
            worker.interrupt()
            try {
                worker.join(WORKER_JOIN_TIMEOUT_MILLIS)
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                cleanupSucceeded = false
            }
            if (worker.isAlive) {
                cleanupSucceeded = false
            } else if (observerStarted) {
                drainQueuedReferences()
            }
        }

        if (workerExitedUnexpectedly) {
            cleanupSucceeded = false
        }
        observerCompleted = cleanupSucceeded
        if (startedAt != null) {
            finishedAfterNanos = elapsedSinceStart()
        }
    }

    private fun elapsedSinceStart(): Long = nanoTime() - requireNotNull(startedAtNanos)

    private fun activityType(activity: Activity): ActivityCollectionType =
        when (activity.javaClass.simpleName) {
            ActivityCollectionType.MainActivity.name -> ActivityCollectionType.MainActivity
            ActivityCollectionType.PaymentSheetActivity.name -> ActivityCollectionType.PaymentSheetActivity
            ActivityCollectionType.PaymentOptionsActivity.name -> ActivityCollectionType.PaymentOptionsActivity
            else -> ActivityCollectionType.Other
        }

    private class MutableActivityCollectionObservation(
        val type: ActivityCollectionType,
        val destroyedAfterNanos: Long,
        var collectionObservedAfterNanos: Long?,
    )

    private companion object {
        const val WORKER_JOIN_TIMEOUT_MILLIS = 1_000L
    }
}
