package com.stripe.android.paymentsheet.utils

import android.app.Activity
import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runners.model.Statement

internal class ActivityCollectionProfileTest {
    @Test
    fun recordsActivityCategoriesAndUnregistersAfterLeakCheck() = runScenario {
        val activities = onMainThread {
            listOf(
                MainActivity(),
                PaymentSheetActivity(),
                PaymentOptionsActivity(),
                OtherActivity(),
                PaymentSheetActivity(),
            )
        }
        var callbacks: Application.ActivityLifecycleCallbacks? = null
        val rules = profile.rulesStatement(statement {
            callbacks = application.registerCalls.takeItem()
            assertThat(profile.snapshot().observerStarted).isTrue()

            activities.forEach { activity ->
                advance(10L)
                requireNotNull(callbacks).onActivityDestroyed(activity)
                references.takeItem()
            }

            assertThat(profile.snapshot().activities).hasSize(5)
        })
        val leakCheck = statement {
            rules.evaluate()
            assertThat(profile.snapshot().leakCheckStartedAfterNanos).isEqualTo(50L)
            assertThat(profile.snapshot().observerCompleted).isFalse()
            advance(10L)
        }

        profile.statement(leakCheck).evaluate()

        val diagnostics = profile.snapshot()
        assertThat(diagnostics.observerStarted).isTrue()
        assertThat(diagnostics.observerCompleted).isTrue()
        assertThat(diagnostics.workerExitedUnexpectedly).isFalse()
        assertThat(diagnostics.leakCheckStartedAfterNanos).isEqualTo(50L)
        assertThat(diagnostics.leakCheckFinishedAfterNanos).isEqualTo(60L)
        assertThat(diagnostics.finishedAfterNanos).isEqualTo(60L)
        assertThat(diagnostics.activities.map { it.type }).containsExactly(
            ActivityCollectionType.MainActivity,
            ActivityCollectionType.PaymentSheetActivity,
            ActivityCollectionType.PaymentOptionsActivity,
            ActivityCollectionType.Other,
            ActivityCollectionType.PaymentSheetActivity,
        ).inOrder()
        assertThat(diagnostics.activities.map { it.destroyedAfterNanos }).containsExactly(
            10L,
            20L,
            30L,
            40L,
            50L,
        ).inOrder()
        assertThat(diagnostics.activities.map { it.collectionObservedAfterNanos }).containsExactly(
            null,
            null,
            null,
            null,
            null,
        ).inOrder()
        assertThat(activities).hasSize(5)
        assertThat(application.unregisterCalls.takeItem()).isSameInstanceAs(callbacks)
    }

    @Test
    fun manualClearAndEnqueueRecordsCollectionObservationAndStopsWorker() = runScenario {
        var callbacks: Application.ActivityLifecycleCallbacks? = null
        var beforeCollection: ActivityCollectionDiagnostics? = null
        val rules = profile.rulesStatement(statement {
            callbacks = application.registerCalls.takeItem()
            advance(20L)
            val activity = onMainThread { PaymentSheetActivity() }
            requireNotNull(callbacks).onActivityDestroyed(activity)
            val reference = references.takeItem()
            beforeCollection = profile.snapshot()
            assertThat(beforeCollection?.activities?.single()?.collectionObservedAfterNanos).isNull()
            reference.clear()
            assertThat(reference.enqueue()).isTrue()
            assertThat(workerClockEntered.await(5L, TimeUnit.SECONDS)).isTrue()
            assertThat(profile.snapshot().activities.single().collectionObservedAfterNanos).isEqualTo(20L)
        })

        profile.statement(rules).evaluate()

        val diagnostics = profile.snapshot()
        assertThat(diagnostics.activities).hasSize(1)
        assertThat(diagnostics.activities.single().type).isEqualTo(ActivityCollectionType.PaymentSheetActivity)
        assertThat(diagnostics.activities.single().destroyedAfterNanos).isEqualTo(20L)
        assertThat(diagnostics.activities.single().collectionObservedAfterNanos).isEqualTo(20L)
        assertThat(diagnostics.observerCompleted).isTrue()
        assertThat(diagnostics.workerExitedUnexpectedly).isFalse()
        assertThat(requireNotNull(beforeCollection).activities.single().collectionObservedAfterNanos).isNull()
        assertThat(application.unregisterCalls.takeItem()).isSameInstanceAs(callbacks)
    }

    @Test
    fun recordsReferencesAlreadyQueuedWhenTheObserverShutsDown() = runScenario {
        var callbacks: Application.ActivityLifecycleCallbacks? = null
        val rules = profile.rulesStatement(statement {
            callbacks = application.registerCalls.takeItem()
            advance(20L)
            val activity = onMainThread { OtherActivity() }
            requireNotNull(callbacks).onActivityDestroyed(activity)
            val reference = references.takeItem()
            reference.clear()
            assertThat(reference.enqueue()).isTrue()
        })

        profile.statement(rules).evaluate()

        val diagnostics = profile.snapshot()
        assertThat(diagnostics.activities.single().type).isEqualTo(ActivityCollectionType.Other)
        assertThat(diagnostics.activities.single().collectionObservedAfterNanos).isEqualTo(20L)
        assertThat(diagnostics.observerCompleted).isTrue()
        assertThat(diagnostics.workerExitedUnexpectedly).isFalse()
        assertThat(application.unregisterCalls.takeItem()).isSameInstanceAs(callbacks)
    }

    @Test
    fun sharedRulesFailureDoesNotStartLeakCheckAndPreservesFailure() = runScenario {
        val failure = IllegalStateException("shared rules failed")
        var callbacks: Application.ActivityLifecycleCallbacks? = null
        val testRules = profile.rulesStatement(statement {
            callbacks = application.registerCalls.takeItem()
            throw failure
        })

        val thrown = assertThrows(IllegalStateException::class.java) {
            profile.statement(testRules).evaluate()
        }

        assertThat(thrown).isSameInstanceAs(failure)
        val diagnostics = profile.snapshot()
        assertThat(diagnostics.observerStarted).isTrue()
        assertThat(diagnostics.observerCompleted).isTrue()
        assertThat(diagnostics.leakCheckStartedAfterNanos).isNull()
        assertThat(diagnostics.leakCheckFinishedAfterNanos).isNull()
        assertThat(diagnostics.activities).isEmpty()
        assertThat(application.unregisterCalls.takeItem()).isSameInstanceAs(callbacks)
    }

    @Test
    fun leakCheckFailureRecordsCompletionAndPreservesFailure() = runScenario {
        val failure = AssertionError("leak check failed")
        var callbacks: Application.ActivityLifecycleCallbacks? = null
        val testRules = profile.rulesStatement(statement {
            callbacks = application.registerCalls.takeItem()
            advance(15L)
        })
        val leakCheck = statement {
            testRules.evaluate()
            advance(25L)
            throw failure
        }

        val thrown = assertThrows(AssertionError::class.java) {
            profile.statement(leakCheck).evaluate()
        }

        assertThat(thrown).isSameInstanceAs(failure)
        val diagnostics = profile.snapshot()
        assertThat(diagnostics.leakCheckStartedAfterNanos).isEqualTo(15L)
        assertThat(diagnostics.leakCheckFinishedAfterNanos).isEqualTo(40L)
        assertThat(diagnostics.finishedAfterNanos).isEqualTo(40L)
        assertThat(diagnostics.observerCompleted).isTrue()
        assertThat(application.unregisterCalls.takeItem()).isSameInstanceAs(callbacks)
    }

    @Test
    fun interruptedWorkerJoinRestoresInterruptAndMarksDiagnosticsIncomplete() = runScenario {
        var callbacks: Application.ActivityLifecycleCallbacks? = null
        val rules = profile.rulesStatement(statement {
            callbacks = application.registerCalls.takeItem()
            advance(10L)
            val activity = onMainThread { PaymentSheetActivity() }
            requireNotNull(callbacks).onActivityDestroyed(activity)
            val reference = references.takeItem()
            reference.clear()
            blockWorkerClock = true
            reference.enqueue()
            assertThat(workerClockEntered.await(5L, TimeUnit.SECONDS)).isTrue()
        })
        val leakCheck = statement {
            rules.evaluate()
            Thread.currentThread().interrupt()
        }

        var interruptRestored = false
        try {
            profile.statement(leakCheck).evaluate()
            interruptRestored = Thread.currentThread().isInterrupted
        } finally {
            interruptRestored = Thread.interrupted() || interruptRestored
            releaseWorkerClock.countDown()
        }

        val diagnostics = profile.snapshot()
        assertThat(interruptRestored).isTrue()
        assertThat(diagnostics.observerStarted).isTrue()
        assertThat(diagnostics.observerCompleted).isFalse()
        assertThat(diagnostics.workerExitedUnexpectedly).isFalse()
        assertThat(application.unregisterCalls.takeItem()).isSameInstanceAs(callbacks)
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        val scenario = Scenario()
        scenario.block()
        scenario.ensureAllEventsConsumed()
    }

    private fun <T : Any> onMainThread(block: () -> T): T {
        lateinit var result: T
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result = block()
        }
        return result
    }

    private class Scenario {
        private val testThread = Thread.currentThread()

        @Volatile
        var nowNanos = 1_000L

        @Volatile
        var blockWorkerClock = false

        val workerClockEntered = CountDownLatch(1)
        val releaseWorkerClock = CountDownLatch(1)
        val application = FakeApplication()
        val references = Turbine<WeakReference<Activity>>()
        val profile = ActivityCollectionProfile(
            application = application,
            nanoTime = ::nanoTime,
            referenceFactory = ::createReference,
        )

        fun advance(nanos: Long) {
            nowNanos += nanos
        }

        fun statement(block: () -> Unit): Statement = object : Statement() {
            override fun evaluate() = block()
        }

        fun ensureAllEventsConsumed() {
            application.registerCalls.ensureAllEventsConsumed()
            application.unregisterCalls.ensureAllEventsConsumed()
            references.ensureAllEventsConsumed()
        }

        private fun nanoTime(): Long {
            if (Thread.currentThread() !== testThread) {
                workerClockEntered.countDown()
                if (blockWorkerClock) {
                    var interrupted = false
                    while (true) {
                        try {
                            releaseWorkerClock.await()
                            break
                        } catch (_: InterruptedException) {
                            interrupted = true
                        }
                    }
                    if (interrupted) Thread.currentThread().interrupt()
                }
            }
            return nowNanos
        }

        private fun createReference(
            activity: Activity,
            queue: ReferenceQueue<Activity>,
        ): WeakReference<Activity> {
            val reference = WeakReference(activity, queue)
            references.add(reference)
            return reference
        }
    }

    private class FakeApplication : Application() {
        val registerCalls = Turbine<Application.ActivityLifecycleCallbacks>()
        val unregisterCalls = Turbine<Application.ActivityLifecycleCallbacks>()

        override fun registerActivityLifecycleCallbacks(callback: Application.ActivityLifecycleCallbacks) {
            registerCalls.add(callback)
        }

        override fun unregisterActivityLifecycleCallbacks(callback: Application.ActivityLifecycleCallbacks) {
            unregisterCalls.add(callback)
        }
    }

    private class MainActivity : Activity()
    private class PaymentSheetActivity : Activity()
    private class PaymentOptionsActivity : Activity()
    private class OtherActivity : Activity()
}
