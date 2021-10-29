package com.stripe.android.cardverificationsheet.framework

import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import com.stripe.android.cardverificationsheet.framework.time.Duration
import com.stripe.android.cardverificationsheet.framework.time.nanoseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class LoopTest {

    @Test(timeout = 1000)
    @SmallTest
    @ExperimentalCoroutinesApi
    fun processBoundAnalyzerLoop_analyzeData() = runBlockingTest {
        val dataCount = 3
        val resultCount = AtomicInteger(0)

        class TestResultHandler : StatefulResultHandler<Int, Int, String, Boolean>(1) {
            override suspend fun onResult(result: String, data: Int): Boolean {
                assertEquals(1, state)
                val count = resultCount.incrementAndGet()
                return count >= dataCount
            }
        }

        val analyzerPool = AnalyzerPool.of(
            analyzerFactory = TestAnalyzerFactory(),
            desiredAnalyzerCount = 12
        )

        val loop = ProcessBoundAnalyzerLoop(
            analyzerPool = analyzerPool,
            analyzerLoopErrorListener = object : AnalyzerLoopErrorListener {
                override fun onAnalyzerFailure(t: Throwable): Boolean { fail(t.message) }
                override fun onResultFailure(t: Throwable): Boolean { fail(t.message) }
            },
            resultHandler = TestResultHandler()
        )

        val channel = Channel<Int>(dataCount)
        val job = loop.subscribeTo(channel.receiveAsFlow(), this)
        assertNotNull(job)

        repeat(dataCount) {
            while (!channel.trySend(it).isSuccess) {
                // loop until the channel accepts the data
            }
        }

        job.joinTest()
        assertTrue { dataCount == resultCount.get() }
    }

    @Test(timeout = 200)
    @SmallTest
    @ExperimentalCoroutinesApi
    fun processBoundAnalyzerLoop_analyzeDataNoDuplicates() = runBlockingTest {
        val dataCount = 3
        var resultCount = 0

        val dataProcessMutex = Mutex()
        val dataToProcess = mutableMapOf<Int, Boolean>()
        repeat(dataCount) {
            dataToProcess[it] = false
        }

        class TestResultHandler : StatefulResultHandler<Int, Int, String, Boolean>(1) {
            override suspend fun onResult(result: String, data: Int): Boolean {
                dataProcessMutex.withLock {
                    resultCount++
                    assertEquals(1, state)
                    assertTrue { dataToProcess[data] == false }
                    dataToProcess[data] = true
                    return resultCount >= dataCount
                }
            }
        }

        val analyzerPool = AnalyzerPool.of(
            analyzerFactory = TestAnalyzerFactory(),
            desiredAnalyzerCount = 12
        )

        val loop = ProcessBoundAnalyzerLoop(
            analyzerPool = analyzerPool,
            analyzerLoopErrorListener = object : AnalyzerLoopErrorListener {
                override fun onAnalyzerFailure(t: Throwable): Boolean { fail(t.message) }
                override fun onResultFailure(t: Throwable): Boolean { fail(t.message) }
            },
            resultHandler = TestResultHandler()
        )

        val channel = Channel<Int>(dataCount)
        val job = loop.subscribeTo(channel.receiveAsFlow(), this)
        assertNotNull(job)

        repeat(dataCount) {
            while (!channel.trySend(it).isSuccess) {
                // loop until the channel accepts the data
            }
        }

        job.joinTest()
        val dataProcessedCount = dataProcessMutex.withLock { resultCount }
        assertTrue { dataCount == dataProcessedCount }

        repeat(dataCount) {
            assertTrue { dataToProcess[it] == true }
        }
    }

    @Test(timeout = 200)
    @SmallTest
    @ExperimentalCoroutinesApi
    fun processBoundAnalyzerLoop_noAnalyzersAvailable() = runBlockingTest {
        var analyzerFailure = false

        class TestResultHandler : StatefulResultHandler<Int, Int, String, Boolean>(1) {
            override suspend fun onResult(result: String, data: Int): Boolean {
                assertEquals(1, state)
                return false
            }
        }

        val analyzerPool = AnalyzerPool.of(
            analyzerFactory = TestAnalyzerFactory(),
            desiredAnalyzerCount = 0
        )

        val loop = ProcessBoundAnalyzerLoop(
            analyzerPool = analyzerPool,
            analyzerLoopErrorListener = object : AnalyzerLoopErrorListener {
                override fun onAnalyzerFailure(t: Throwable): Boolean {
                    analyzerFailure = true
                    return true
                }
                override fun onResultFailure(t: Throwable): Boolean { fail(t.message) }
            },
            resultHandler = TestResultHandler()
        )

        val channel = Channel<Int>(Channel.RENDEZVOUS)
        val job = loop.subscribeTo(channel.receiveAsFlow(), this)
        assertNull(job)
        assertTrue { analyzerFailure }
    }

    @Test(timeout = 1000)
    @SmallTest
    @ExperimentalCoroutinesApi
    @Ignore("This test is flaking in CI")
    fun finiteAnalyzerLoop_analyzeData() = runBlockingTest {
        val dataCount = 3
        var dataProcessed = false
        val resultCount = AtomicInteger(0)

        class TestResultHandler : TerminatingResultHandler<Int, Int, String>(1) {
            override suspend fun onResult(result: String, data: Int) {
                assertEquals(1, state)
                resultCount.incrementAndGet()
            }

            override suspend fun onAllDataProcessed() {
                assertEquals(dataCount, resultCount.get())
                dataProcessed = true
            }

            override suspend fun onTerminatedEarly() {
                fail()
            }
        }

        val analyzerPool = AnalyzerPool.of(
            analyzerFactory = TestAnalyzerFactory(),
            desiredAnalyzerCount = 12
        )

        val loop = FiniteAnalyzerLoop(
            analyzerPool = analyzerPool,
            analyzerLoopErrorListener = object : AnalyzerLoopErrorListener {
                override fun onAnalyzerFailure(t: Throwable): Boolean { fail(t.message) }
                override fun onResultFailure(t: Throwable): Boolean { fail(t.message) }
            },
            resultHandler = TestResultHandler(),
            timeLimit = Duration.INFINITE
        )

        val job = loop.process((0 until dataCount).map { 2 }, this)
        assertNotNull(job)
        job.joinTest()

        assertTrue(dataProcessed)
    }

    @Test(timeout = 1000)
    @MediumTest
    @ExperimentalCoroutinesApi
    fun finiteAnalyzerLoop_analyzeDataTimeout() = runBlockingTest {
        val dataCount = 10000
        val resultCount = AtomicInteger(0)
        var terminatedEarly = false

        class TestResultHandler : TerminatingResultHandler<Int, Int, String>(1) {
            override suspend fun onResult(result: String, data: Int) {
                assertEquals(1, state)
                resultCount.incrementAndGet()
            }

            override suspend fun onAllDataProcessed() {
                fail()
            }

            override suspend fun onTerminatedEarly() {
                assertTrue { resultCount.get() < dataCount }
                terminatedEarly = true
            }
        }

        val analyzerPool = AnalyzerPool.of(
            analyzerFactory = TestAnalyzerFactory(),
            desiredAnalyzerCount = 12
        )

        val loop = FiniteAnalyzerLoop(
            analyzerPool = analyzerPool,
            analyzerLoopErrorListener = object : AnalyzerLoopErrorListener {
                override fun onAnalyzerFailure(t: Throwable): Boolean { fail(t.message) }
                override fun onResultFailure(t: Throwable): Boolean { fail(t.message) }
            },
            resultHandler = TestResultHandler(),
            timeLimit = 1.nanoseconds
        )

        val job = loop.process((0 until dataCount).map { 2 }, this)
        assertNotNull(job)
        job.joinTest()

        assertTrue { terminatedEarly }
    }

    @Test(timeout = 200)
    @SmallTest
    @ExperimentalCoroutinesApi
    fun finiteAnalyzerLoop_analyzeDataNoData() = runBlockingTest {
        var dataProcessed = false

        class TestResultHandler : TerminatingResultHandler<Int, Int, String>(1) {
            override suspend fun onResult(result: String, data: Int) { fail() }

            override suspend fun onAllDataProcessed() { dataProcessed = true }

            override suspend fun onTerminatedEarly() { fail() }
        }

        val analyzerPool = AnalyzerPool.of(
            analyzerFactory = TestAnalyzerFactory(),
            desiredAnalyzerCount = 12
        )

        val loop = FiniteAnalyzerLoop(
            analyzerPool = analyzerPool,
            analyzerLoopErrorListener = object : AnalyzerLoopErrorListener {
                override fun onAnalyzerFailure(t: Throwable): Boolean { fail(t.message) }
                override fun onResultFailure(t: Throwable): Boolean { fail(t.message) }
            },
            resultHandler = TestResultHandler(),
            timeLimit = Duration.INFINITE
        )

        val job = loop.process(emptyList(), this)
        assertNotNull(job)
        job.joinTest()

        assertTrue { dataProcessed }
    }

    private class TestAnalyzer : Analyzer<Int, Int, String> {
        companion object {
            private val analyzerCounter = AtomicInteger(0)
        }

        private val analyzerNumber = analyzerCounter.getAndIncrement()
        override suspend fun analyze(data: Int, state: Int): String =
            "Analyzer=$analyzerNumber, data=$data, state=$state"
    }

    private class TestAnalyzerFactory : AnalyzerFactory<Int, Int, String, TestAnalyzer> {
        override suspend fun newInstance(): TestAnalyzer? = TestAnalyzer()
    }

    private suspend fun Job.joinTest() {
        while (!isCompleted) {
            yield()
        }
        join()
    }
}
