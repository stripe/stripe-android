package com.stripe.android.common.coroutines

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test

@OptIn(DelicateCoroutinesApi::class)
internal class CoalescingOrchestratorTest {
    @Test
    fun testGetReturnsData(): Unit = runBlocking {
        val subject = CoalescingOrchestrator({ "it works" }, { true })
        assertThat(subject.get()).isEqualTo("it works")
    }

    @Test
    fun testGetOnlyCallsFactoryOnceWhenStoringInMemory(): Unit = runBlocking {
        val factoryCount = AtomicInteger(0)
        val subject = CoalescingOrchestrator(
            factory = {
                factoryCount.incrementAndGet()
                "it works"
            },
            keepDataInMemory = { true }
        )
        assertThat(subject.get()).isEqualTo("it works")
        assertThat(subject.get()).isEqualTo("it works")
        assertThat(factoryCount.get()).isEqualTo(1)
    }

    @Test
    fun testGetCallsFactoryTwiceWhenNotStoringInMemory(): Unit = runBlocking {
        val factoryCount = AtomicInteger(0)
        val subject = CoalescingOrchestrator(
            factory = {
                "it works ${factoryCount.getAndIncrement()}"
            },
            keepDataInMemory = { false }
        )
        assertThat(subject.get()).isEqualTo("it works 0")
        assertThat(subject.get()).isEqualTo("it works 1")
        assertThat(factoryCount.get()).isEqualTo(2)
    }

    @Test
    fun testGetCallsFactoryUntilKeepDataInMemoryIsTrue(): Unit = runBlocking {
        val factoryCount = AtomicInteger(0)
        val keepDataInMemoryCount = AtomicInteger(0)
        val subject = CoalescingOrchestrator(
            factory = {
                "it works ${factoryCount.getAndIncrement()}"
            },
            keepDataInMemory = {
                keepDataInMemoryCount.getAndIncrement()
                it == "it works 1"
            }
        )
        assertThat(subject.get()).isEqualTo("it works 0")
        assertThat(subject.get()).isEqualTo("it works 1")
        assertThat(subject.get()).isEqualTo("it works 1")
        assertThat(subject.get()).isEqualTo("it works 1")
        assertThat(factoryCount.get()).isEqualTo(2)
        assertThat(keepDataInMemoryCount.get()).isEqualTo(2)
    }

    @Test
    fun testCancellation() {
        val factoryCount = AtomicInteger(0)
        var countDownLatch = CountDownLatch(1)
        val deferred = CompletableDeferred<String>()
        val subject = CoalescingOrchestrator(
            factory = {
                factoryCount.incrementAndGet()
                countDownLatch.countDown()
                deferred.await()
            },
            keepDataInMemory = { true },
        )

        val job1 = GlobalScope.launch(Dispatchers.IO) {
            subject.get()
        }
        runBlocking {
            assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
            job1.cancelAndJoin()
        }
        countDownLatch = CountDownLatch(1)

        val job2 = GlobalScope.async(Dispatchers.IO) {
            subject.get()
        }
        runBlocking {
            assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
            deferred.complete("it works")
            assertThat(job2.await()).isEqualTo("it works")
        }
        assertThat(factoryCount.get()).isEqualTo(2)
    }

    @Test
    fun testCancellingOneDoesNotAffectOther() {
        val factoryCount = AtomicInteger(0)
        val deferred = CompletableDeferred<String>()
        val countDownLatch = CountDownLatch(2)
        val subject = CoalescingOrchestrator(
            factory = {
                val result = deferred.await()
                factoryCount.incrementAndGet()
                result
            },
            keepDataInMemory = { false },
            awaitListener = {
                countDownLatch.countDown()
            },
        )

        val job1 = GlobalScope.async(Dispatchers.IO) {
            subject.get()
        }
        val job2 = GlobalScope.async(Dispatchers.IO) {
            subject.get()
        }
        runBlocking {
            assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
            job2.cancelAndJoin()
            deferred.complete("it works")
            assertThat(job1.await()).isEqualTo("it works")
        }

        assertThat(factoryCount.get()).isEqualTo(1)
    }

    @Test
    fun testParallelCallsFactoryOnce() {
        val factoryCount = AtomicInteger(0)
        val deferred = CompletableDeferred<String>()
        val countDownLatch = CountDownLatch(2)
        val subject = CoalescingOrchestrator(
            factory = {
                factoryCount.incrementAndGet()
                deferred.await()
            },
            keepDataInMemory = { false },
            awaitListener = {
                countDownLatch.countDown()
            }
        )

        val job1 = GlobalScope.async(Dispatchers.IO) {
            subject.get()
        }
        val job2 = GlobalScope.async(Dispatchers.IO) {
            subject.get()
        }
        runBlocking {
            assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
            deferred.complete("it works")
            assertThat(job1.await()).isEqualTo("it works")
            assertThat(job2.await()).isEqualTo("it works")
        }

        assertThat(factoryCount.get()).isEqualTo(1)
        assertThat(countDownLatch.count).isEqualTo(0)
    }
}
