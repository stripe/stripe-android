package com.stripe.android.stripecardscan.framework

import androidx.test.filters.SmallTest
import com.stripe.android.camera.framework.Analyzer
import com.stripe.android.camera.framework.AnalyzerFactory
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener
import com.stripe.android.camera.framework.AnalyzerPool
import com.stripe.android.camera.framework.ProcessBoundAnalyzerLoop
import com.stripe.android.camera.framework.StatefulResultHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class LoopTest {

    @Test(timeout = 3000)
    @SmallTest
    @ExperimentalCoroutinesApi
    fun processBoundAnalyzerLoop_analyzeData() = runTest {
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
                println("Waiting for channel to become available to send data $it")
                yield()
            }
        }

        job.joinTest()
        assertTrue { dataCount == resultCount.get() }
    }

    @Test(timeout = 2000)
    @SmallTest
    @ExperimentalCoroutinesApi
    fun processBoundAnalyzerLoop_analyzeDataNoDuplicates() = runTest {
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
                println("Waiting for channel to become available to send data $it")
                yield()
            }
        }

        job.joinTest()
        val dataProcessedCount = dataProcessMutex.withLock { resultCount }
        assertTrue { dataCount == dataProcessedCount }

        repeat(dataCount) {
            assertTrue { dataToProcess[it] == true }
        }
    }

    @Test(timeout = 2000)
    @SmallTest
    @ExperimentalCoroutinesApi
    fun processBoundAnalyzerLoop_noAnalyzersAvailable() = runTest {
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
