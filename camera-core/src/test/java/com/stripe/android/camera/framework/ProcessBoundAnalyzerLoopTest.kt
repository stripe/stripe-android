package com.stripe.android.camera.framework

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class ProcessBoundAnalyzerLoopTest {
    @Test
    fun `loop terminates when one concurrent worker reaches terminal state`() {
        runBlocking {
            val workerCount = FRAMES.size

            val loop = ProcessBoundAnalyzerLoop(
                analyzerPool = AnalyzerPool(
                    desiredAnalyzerCount = workerCount,
                    analyzers = List(workerCount) { TestAnalyzer() },
                ),
                analyzerLoopErrorListener = FailOnErrorListener,
                resultHandler = TerminalFrameResultHandler(frameToCompleteOn = FRAME_TO_COMPLETE_ON),
            )

            // Send a frame for the loop to analyze
            val framesChannel = Channel<Int>(FRAMES.size).apply {
                FRAMES.forEach { frame ->
                    assertThat(trySend(frame).isSuccess).isTrue()
                }
            }

            val job = requireNotNull(
                loop.subscribeTo(framesChannel.receiveAsFlow(), this)
            )

            assertThat(withTimeout(TERMINATION_TIMEOUT_MILLIS) { job.join() }).isNotNull()

            assertThat(job.isCompleted).isTrue()
            framesChannel.close()
        }
    }

    private class TerminalFrameResultHandler(
        val frameToCompleteOn: Int,
    ) : StatefulResultHandler<Int, Int, String, Boolean>(initialState = 0) {
        override suspend fun onResult(result: String, data: Int): Boolean =
            data == frameToCompleteOn
    }

    private object FailOnErrorListener : AnalyzerLoopErrorListener {
        override fun onAnalyzerFailure(t: Throwable): Boolean = throw AssertionError(t)
        override fun onResultFailure(t: Throwable): Boolean = throw AssertionError(t)
    }

    private class TestAnalyzer : Analyzer<Int, Int, String> {
        override suspend fun analyze(data: Int, state: Int): String = "data=$data, state=$state"
    }

    private companion object {
        val FRAMES = (1 until 1500).toList()

        const val FRAME_TO_COMPLETE_ON = 854

        const val TERMINATION_TIMEOUT_MILLIS = 2_000L
    }
}
