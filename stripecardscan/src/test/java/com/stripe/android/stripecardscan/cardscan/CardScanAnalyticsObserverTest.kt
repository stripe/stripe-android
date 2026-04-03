package com.stripe.android.stripecardscan.cardscan

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripecardscan.cardscan.result.MainLoopState
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

internal class CardScanAnalyticsObserverTest {

    private val timeSource = TestTimeSource()

    @Test
    fun `buildAnalyticsData returns defaults when no transitions occurred`() = runScenario {
        val data = observer.buildAnalyticsData(
            totalFramesProcessed = 0,
            averageFrameRateHz = null,
        )

        assertThat(data.mlKitEnabled).isTrue()
        assertThat(data.panFound).isFalse()
        assertThat(data.expiryFound).isFalse()
        assertThat(data.finishReason).isNull()
        assertThat(data.timeToFirstDetectionMs).isNull()
        assertThat(data.stateResetCount).isEqualTo(0)
    }

    @Test
    fun `tracks time to first detection on Initial to OcrFound`() = runScenario {
        timeSource += 1.5.seconds

        observer.onStateTransition(
            previousState = MainLoopState.Initial(timeSource),
            newState = MainLoopState.OcrFound(timeSource, pan = "4242424242424242"),
        )

        val data = observer.buildAnalyticsData(totalFramesProcessed = 1, averageFrameRateHz = null)
        assertThat(data.timeToFirstDetectionMs).isEqualTo(1500)
    }

    @Test
    fun `time to first detection is only set once`() = runScenario {
        val initial = MainLoopState.Initial(timeSource)
        val ocrFound = MainLoopState.OcrFound(timeSource, pan = "4242424242424242")

        timeSource += 1.seconds
        observer.onStateTransition(initial, ocrFound)

        // Reset and re-detect
        observer.onStateTransition(ocrFound, MainLoopState.Initial(timeSource))
        timeSource += 5.seconds
        observer.onStateTransition(
            MainLoopState.Initial(timeSource),
            MainLoopState.OcrFound(timeSource, pan = "4242424242424242"),
        )

        val data = observer.buildAnalyticsData(totalFramesProcessed = 3, averageFrameRateHz = null)
        assertThat(data.timeToFirstDetectionMs).isEqualTo(1000)
    }

    @Test
    fun `increments stateResetCount on OcrFound to Initial`() = runScenario {
        val initial = MainLoopState.Initial(timeSource)
        val ocrFound = MainLoopState.OcrFound(timeSource, pan = "4242424242424242")

        observer.onStateTransition(initial, ocrFound)
        observer.onStateTransition(ocrFound, MainLoopState.Initial(timeSource))
        observer.onStateTransition(MainLoopState.Initial(timeSource), ocrFound)
        observer.onStateTransition(ocrFound, MainLoopState.Initial(timeSource))

        val data = observer.buildAnalyticsData(totalFramesProcessed = 4, averageFrameRateHz = null)
        assertThat(data.stateResetCount).isEqualTo(2)
    }

    @Test
    fun `derives ocr_agreement finish reason from OcrFound to Finished`() = runScenario {
        val ocrFound = MainLoopState.OcrFound(timeSource, pan = "4242424242424242")
        val finished = MainLoopState.Finished(timeSource, pan = "4242424242424242")

        observer.onStateTransition(MainLoopState.Initial(timeSource), ocrFound)
        observer.onStateTransition(ocrFound, finished)

        val data = observer.buildAnalyticsData(totalFramesProcessed = 3, averageFrameRateHz = null)
        assertThat(data.finishReason).isEqualTo(CardScanAnalyticsData.FINISH_REASON_OCR_AGREEMENT)
        assertThat(data.panFound).isTrue()
    }

    @Test
    fun `derives timeout finish reason when OCR search duration exceeded`() = runScenario {
        val ocrFound = MainLoopState.OcrFound(timeSource, pan = "4242424242424242")

        observer.onStateTransition(MainLoopState.Initial(timeSource), ocrFound)

        // Simulate time passing beyond OCR_SEARCH_DURATION
        timeSource += MainLoopState.OCR_SEARCH_DURATION + 1.seconds

        val finished = MainLoopState.Finished(timeSource, pan = "4242424242424242")
        observer.onStateTransition(ocrFound, finished)

        val data = observer.buildAnalyticsData(totalFramesProcessed = 5, averageFrameRateHz = null)
        assertThat(data.finishReason).isEqualTo(CardScanAnalyticsData.FINISH_REASON_TIMEOUT)
    }

    @Test
    fun `derives expiry_found finish reason from ExpiryWait to Finished with expiry`() = runScenario {
        val expiryWait = MainLoopState.ExpiryWait(timeSource, pan = "4242424242424242")
        val finished = MainLoopState.Finished(
            timeSource, pan = "4242424242424242", expiryMonth = 12, expiryYear = 2028,
        )

        observer.onStateTransition(expiryWait, finished)

        val data = observer.buildAnalyticsData(totalFramesProcessed = 4, averageFrameRateHz = null)
        assertThat(data.finishReason).isEqualTo(CardScanAnalyticsData.FINISH_REASON_EXPIRY_FOUND)
        assertThat(data.expiryFound).isTrue()
    }

    @Test
    fun `derives expiry_wait_timeout finish reason from ExpiryWait to Finished without expiry`() = runScenario {
        val expiryWait = MainLoopState.ExpiryWait(timeSource, pan = "4242424242424242")
        val finished = MainLoopState.Finished(timeSource, pan = "4242424242424242")

        observer.onStateTransition(expiryWait, finished)

        val data = observer.buildAnalyticsData(totalFramesProcessed = 4, averageFrameRateHz = null)
        assertThat(data.finishReason).isEqualTo(CardScanAnalyticsData.FINISH_REASON_EXPIRY_WAIT_TIMEOUT)
        assertThat(data.expiryFound).isFalse()
    }

    @Test
    fun `passes through frame metrics`() = runScenario {
        val data = observer.buildAnalyticsData(
            totalFramesProcessed = 42,
            averageFrameRateHz = 15.5f,
        )

        assertThat(data.totalFramesProcessed).isEqualTo(42)
        assertThat(data.averageFrameRateHz).isEqualTo(15.5f)
    }

    @Test
    fun `mlKitEnabled reflects constructor parameter`() {
        val observerWithMlKit = CardScanAnalyticsObserver(mlKitEnabled = true, timeSource = timeSource)
        val observerWithoutMlKit = CardScanAnalyticsObserver(mlKitEnabled = false, timeSource = timeSource)

        assertThat(
            observerWithMlKit.buildAnalyticsData(0, null).mlKitEnabled
        ).isTrue()
        assertThat(
            observerWithoutMlKit.buildAnalyticsData(0, null).mlKitEnabled
        ).isFalse()
    }

    private fun runScenario(
        mlKitEnabled: Boolean = true,
        block: Scenario.() -> Unit,
    ) {
        val observer = CardScanAnalyticsObserver(
            mlKitEnabled = mlKitEnabled,
            timeSource = timeSource,
        )
        Scenario(observer).apply { block() }
    }

    private data class Scenario(
        val observer: CardScanAnalyticsObserver,
    )
}
