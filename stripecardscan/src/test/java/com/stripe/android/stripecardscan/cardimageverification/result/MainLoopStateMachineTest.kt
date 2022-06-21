package com.stripe.android.stripecardscan.cardimageverification.result

import androidx.test.filters.LargeTest
import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.camera.framework.time.milliseconds
import com.stripe.android.stripecardscan.cardimageverification.analyzer.MainLoopAnalyzer
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopState.Companion.DESIRED_CARD_COUNT
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopState.Companion.DESIRED_OCR_AGREEMENT
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopState.Companion.NO_CARD_VISIBLE_DURATION
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopState.Companion.OCR_AND_CARD_SEARCH_DURATION
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopState.Companion.OCR_ONLY_SEARCH_DURATION
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopState.Companion.WRONG_CARD_DURATION
import com.stripe.android.stripecardscan.framework.util.ItemCounter
import com.stripe.android.stripecardscan.payment.card.CardIssuer
import com.stripe.android.stripecardscan.payment.ml.CardDetect
import com.stripe.android.stripecardscan.payment.ml.SSDOcr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MainLoopStateMachineTest {

    @Test
    fun `initial state runs OCR and CardDetector ML models`() {
        val state = MainLoopState.Initial(
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        assertTrue(state.runOcr)
        assertTrue(state.runCardDetect)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `initial state does not transition when no card and no OCR are found`() = runTest {
        val state = MainLoopState.Initial(
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = null
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.Initial)
        assertEquals(CardIssuer.Visa, newState.requiredCardIssuer)
        assertEquals("8770", newState.requiredLastFour)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `initial state transitions to wrong card when a wrong card is detected`() = runTest {
        val state = MainLoopState.Initial(
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("5445435282861343"),
            card = null
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.WrongCard)
        assertEquals("8770", newState.requiredLastFour)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `initial state does not transition when OCR is found but no card is visible`() = runTest {
        val state = MainLoopState.Initial(
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 1
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.Initial)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `initial state doesn't transition when OCR is not found but a card is visible`() = runTest {
        val state = MainLoopState.Initial(
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 1
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = CardDetect.Prediction(
                side = CardDetect.Prediction.Side.PAN,
                noCardProbability = 0F,
                noPanProbability = 0F,
                panProbability = 1F
            )
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.Initial)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `initial state transitions when OCR is found and a card is visible`() = runTest {
        val state = MainLoopState.Initial(
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 1
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = CardDetect.Prediction(
                side = CardDetect.Prediction.Side.PAN,
                noCardProbability = 0F,
                noPanProbability = 0F,
                panProbability = 1F
            )
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.OcrFound)
    }

    @Test
    fun `ocrFound runs CardDetect and OCR ML models`() {
        val state = MainLoopState.OcrFound(
            panCounter = ItemCounter("4847186095118770"),
            visibleCardCount = 1,
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        assertTrue(state.runOcr)
        assertTrue(state.runCardDetect)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `ocrFound doesn't transition if it hasn't timed out and hasn't found more OCR`() = runTest {
        val state = MainLoopState.OcrFound(
            panCounter = ItemCounter("4847186095118770"),
            visibleCardCount = 1,
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.OcrFound)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `ocrFound transitions to CardSatisfied when enough matching cards are found`() = runTest {
        var state: MainLoopState = MainLoopState.OcrFound(
            panCounter = ItemCounter("4847186095118770"),
            visibleCardCount = 1,
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = CardDetect.Prediction(
                side = CardDetect.Prediction.Side.PAN,
                panProbability = 1.0F,
                noPanProbability = 0.0F,
                noCardProbability = 0.0F
            )
        )

        // this is -2 because it must be:
        // 1 for initial count
        // + 1 to stay below desired before last transition
        // the last transition will meet the desired count
        repeat(DESIRED_CARD_COUNT - 2) {
            state = state.consumeTransition(prediction)
            assertTrue(state is MainLoopState.OcrFound)
        }

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.CardSatisfied)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `ocrFound transitions to OcrSatisfied when enough pans are found`() = runTest {
        var state: MainLoopState = MainLoopState.OcrFound(
            panCounter = ItemCounter("4847186095118770"),
            visibleCardCount = 1,
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null
        )

        // this is -2 because it must be:
        // 1 for initial count
        // + 1 to stay below desired before last transition
        // the last transition will meet the desired count
        repeat(DESIRED_OCR_AGREEMENT - 2) {
            state = state.consumeTransition(prediction)
            assertTrue(state is MainLoopState.OcrFound)
        }

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.OcrSatisfied)
    }

    /**
     * This test cannot use `runBlockingTest` because it requires a delay. While runBlockingTest
     * advances the dispatcher's virtual time by the specified amount, it does not affect the timing
     * of the duration.
     */
    @Test
    @ExperimentalCoroutinesApi
    fun `OcrFound transitions to Finished when it times out`() = runBlocking {
        val mockClockMark = mock<ClockMark>()
        Mockito.mockStatic(Clock::class.java).use { mockClock ->
            mockClock.`when`<ClockMark> { Clock.markNow() }.thenReturn(mockClockMark)

            var state: MainLoopState = MainLoopState.OcrFound(
                panCounter = ItemCounter("4847186095118770"),
                visibleCardCount = 1,
                requiredCardIssuer = CardIssuer.Visa,
                requiredLastFour = "8770",
                strictModeFrames = 0
            )

            val prediction = MainLoopAnalyzer.Prediction(
                ocr = SSDOcr.Prediction("4847186095118770"),
                card = null
            )

            // this is -3 because it must be:
            // 1 for initial count
            // + 1 to stay below desired before last transition
            // + 1 to stay below desired with the last transition
            repeat(DESIRED_OCR_AGREEMENT - 3) {
                state = state.consumeTransition(prediction)
                assertTrue(state is MainLoopState.OcrFound)
            }

            whenever(mockClockMark.elapsedSince())
                .thenReturn(OCR_AND_CARD_SEARCH_DURATION + 1.milliseconds)

            val newState = state.consumeTransition(prediction)
            assertTrue(newState is MainLoopState.Finished)
        }
    }

    /**
     * This test cannot use `runBlockingTest` because it requires a delay. While runBlockingTest
     * advances the dispatcher's virtual time by the specified amount, it does not affect the timing
     * of the duration.
     */
    @Test
    @ExperimentalCoroutinesApi
    fun `OcrFound transitions to Initial if no card is visible after timeout`() = runBlocking {
        val mockClockMark = mock<ClockMark>()
        Mockito.mockStatic(Clock::class.java).use { mockClock ->
            mockClock.`when`<ClockMark> { Clock.markNow() }.thenReturn(mockClockMark)

            var state: MainLoopState = MainLoopState.OcrFound(
                panCounter = ItemCounter("4847186095118770"),
                visibleCardCount = 1,
                requiredCardIssuer = CardIssuer.Visa,
                requiredLastFour = "8770",
                strictModeFrames = 0
            )

            val predictionWithCard = MainLoopAnalyzer.Prediction(
                ocr = SSDOcr.Prediction("4847186095118770"),
                card = null
            )

            whenever(mockClockMark.elapsedSince())
                .thenReturn(NO_CARD_VISIBLE_DURATION - 1.milliseconds)

            repeat(DESIRED_OCR_AGREEMENT - 2) {
                state = state.consumeTransition(predictionWithCard)
                assertTrue(state is MainLoopState.OcrFound)
            }

            whenever(mockClockMark.elapsedSince())
                .thenReturn(NO_CARD_VISIBLE_DURATION + 1.milliseconds)

            val predictionWithoutCard = MainLoopAnalyzer.Prediction(
                ocr = null,
                card = CardDetect.Prediction(CardDetect.Prediction.Side.NO_CARD, 1.0F, 0.0F, 0.0F)
            )

            val newState = state.consumeTransition(predictionWithoutCard)
            assertTrue(newState is MainLoopState.Initial)
        }
    }

    /**
     * This test cannot use `runBlockingTest` because it requires a delay. While runBlockingTest
     * advances the dispatcher's virtual time by the specified amount, it does not affect the timing
     * of the duration.
     */
    @Test
    @LargeTest
    fun `OcrFound transitions to Finished after a timeout if cards are visible`() = runBlocking {
        val mockClockMark = mock<ClockMark>()
        Mockito.mockStatic(Clock::class.java).use { mockClock ->
            mockClock.`when`<ClockMark> { Clock.markNow() }.thenReturn(mockClockMark)

            val state = MainLoopState.OcrFound(
                panCounter = ItemCounter("4847186095118770"),
                visibleCardCount = 1,
                requiredCardIssuer = CardIssuer.Visa,
                requiredLastFour = "8770",
                strictModeFrames = 0
            )

            whenever(mockClockMark.elapsedSince())
                .thenReturn(OCR_AND_CARD_SEARCH_DURATION + 1.milliseconds)

            val prediction = MainLoopAnalyzer.Prediction(
                ocr = null,
                card = null
            )

            val newState = state.consumeTransition(prediction)
            assertTrue(newState is MainLoopState.Finished, "$newState is not Finished")
        }
    }

    @Test
    @LargeTest
    fun `OcrFound state ignores wrong card pans`() = runBlocking {
        val state = MainLoopState.OcrFound(
            panCounter = ItemCounter("4847186095118770"),
            visibleCardCount = 1,
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction(pan = "5445435282861343"),
            card = null
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.OcrFound, "$newState is not OcrFound")
    }

    @Test
    fun `OcrSatisfied only runs the CardDetect ML model`() {
        val state = MainLoopState.OcrSatisfied(
            pan = "4847186095118770",
            visibleCardCount = 0
        )

        assertFalse(state.runOcr)
        assertTrue(state.runCardDetect)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `OcrSatisfied doesn't transition if no card detected and it hasn't timed out`() = runTest {
        val state = MainLoopState.OcrSatisfied(
            pan = "4847186095118770",
            visibleCardCount = 0
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = CardDetect.Prediction(
                side = CardDetect.Prediction.Side.PAN,
                panProbability = 1.0F,
                noPanProbability = 0.0F,
                noCardProbability = 0.0F
            )
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.OcrSatisfied)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `OcrSatisfied transitions to Finished when enough cards are seen`() = runTest {
        val state = MainLoopState.OcrSatisfied(
            pan = "4847186095118770",
            visibleCardCount = DESIRED_CARD_COUNT - 1
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = CardDetect.Prediction(
                side = CardDetect.Prediction.Side.PAN,
                panProbability = 1.0F,
                noPanProbability = 0.0F,
                noCardProbability = 0.0F
            )
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.Finished)
    }

    /**
     * This test cannot use `runBlockingTest` because it requires a delay. While runBlockingTest
     * advances the dispatcher's virtual time by the specified amount, it does not affect the timing
     * of the duration.
     */
    @Test
    @LargeTest
    fun `OcrSatisfied transitions to Finished when it times out`() = runBlocking {
        val mockClockMark = mock<ClockMark>()
        Mockito.mockStatic(Clock::class.java).use { mockClock ->
            mockClock.`when`<ClockMark> { Clock.markNow() }.thenReturn(mockClockMark)

            val state = MainLoopState.OcrSatisfied(
                pan = "4847186095118770",
                visibleCardCount = DESIRED_CARD_COUNT - 1
            )

            val prediction = MainLoopAnalyzer.Prediction(
                ocr = null,
                card = null
            )

            whenever(mockClockMark.elapsedSince())
                .thenReturn(NO_CARD_VISIBLE_DURATION + 1.milliseconds)

            val newState = state.consumeTransition(prediction)
            assertTrue(newState is MainLoopState.Finished)
        }
    }

    @Test
    fun `CardSatisfied only runs the OCR ML model`() {
        val state = MainLoopState.CardSatisfied(
            panCounter = ItemCounter("4847186095118770"),
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        assertTrue(state.runOcr)
        assertFalse(state.runCardDetect)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `CardSatisfied doesn't transition if no card and not timed out`() = runTest {
        val state = MainLoopState.CardSatisfied(
            panCounter = ItemCounter("4847186095118770"),
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.CardSatisfied)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `CardSatisfied transitions to Finished if enough pans are found`() = runTest {
        var state: MainLoopState = MainLoopState.CardSatisfied(
            panCounter = ItemCounter("4847186095118770"),
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null
        )

        // this is -2 because it must be:
        // 1 for initial count
        // + 1 to stay below desired before last transition
        // the last transition will meet the desired count
        repeat(DESIRED_OCR_AGREEMENT - 2) {
            state = state.consumeTransition(prediction)
            assertTrue(state is MainLoopState.CardSatisfied)
        }

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.Finished)
    }

    /**
     * This test cannot use `runBlockingTest` because it requires a delay. While runBlockingTest
     * advances the dispatcher's virtual time by the specified amount, it does not affect the timing
     * of the duration.
     */
    @Test
    @LargeTest
    fun `CardSatisfied transitions to Finished after timeout`() = runBlocking {
        val mockClockMark = mock<ClockMark>()
        Mockito.mockStatic(Clock::class.java).use { mockClock ->
            mockClock.`when`<ClockMark> { Clock.markNow() }.thenReturn(mockClockMark)

            val state = MainLoopState.CardSatisfied(
                panCounter = ItemCounter("4847186095118770"),
                requiredCardIssuer = CardIssuer.Visa,
                requiredLastFour = "8770",
                strictModeFrames = 0
            )

            val prediction = MainLoopAnalyzer.Prediction(
                ocr = null,
                card = null
            )

            whenever(mockClockMark.elapsedSince())
                .thenReturn(OCR_ONLY_SEARCH_DURATION + 1.milliseconds)

            val newState = state.consumeTransition(prediction)
            assertTrue(newState is MainLoopState.Finished)
        }
    }

    @Test
    @LargeTest
    fun `CardSatisfied ignores wrong pans`() = runBlocking {
        val state = MainLoopState.CardSatisfied(
            panCounter = ItemCounter("4847186095118770"),
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction(pan = "5445435282861343"),
            card = null
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.CardSatisfied, "$newState is not CardSatisfied")
    }

    @Test
    fun `WrongCard only runs the OCR ML model`() {
        val state = MainLoopState.WrongCard(
            pan = "5445435282861343",
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        assertTrue(state.runOcr)
        assertFalse(state.runCardDetect)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `WrongCard does not transition if it hasn't timed out and no card seen`() = runTest {
        val state = MainLoopState.WrongCard(
            pan = "5445435282861343",
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = null
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.WrongCard)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `WrongCard resets if a new frame with the wrong card is visible`() = runTest {
        val state = MainLoopState.WrongCard(
            pan = "5445435282861343",
            requiredCardIssuer = CardIssuer.MasterCard,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("5445435282861343"),
            card = null
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.WrongCard)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `WrongCard transitions to OcrFound if the right card is seen`() = runTest {
        val state = MainLoopState.WrongCard(
            pan = "5445435282861343",
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
            strictModeFrames = 0
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.OcrFound, "$newState is not PanFound")
    }

    /**
     * This test cannot use `runBlockingTest` because it requires a delay. While runBlockingTest
     * advances the dispatcher's virtual time by the specified amount, it does not affect the timing
     * of the duration.
     */
    @Test
    @LargeTest
    fun `WrongCard transitions to initial when it times out`() = runBlocking {
        val mockClockMark = mock<ClockMark>()
        Mockito.mockStatic(Clock::class.java).use { mockClock ->
            mockClock.`when`<ClockMark> { Clock.markNow() }.thenReturn(mockClockMark)

            val state = MainLoopState.WrongCard(
                pan = "5445435282861343",
                requiredCardIssuer = CardIssuer.Visa,
                requiredLastFour = "8770",
                strictModeFrames = 0
            )

            val prediction = MainLoopAnalyzer.Prediction(
                ocr = null,
                card = null
            )

            whenever(mockClockMark.elapsedSince())
                .thenReturn(WRONG_CARD_DURATION + 1.milliseconds)

            val newState = state.consumeTransition(prediction)
            assertTrue(newState is MainLoopState.Initial)
        }
    }

    @Test
    fun `Finished does not run any ML models`() {
        val state = MainLoopState.Finished("4847186095118770")

        assertFalse(state.runOcr)
        assertFalse(state.runCardDetect)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `Finished does not transition`() = runTest {
        val state = MainLoopState.Finished("4847186095118770")

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = CardDetect.Prediction(
                side = CardDetect.Prediction.Side.NO_CARD,
                panProbability = 0.0F,
                noPanProbability = 0.0F,
                noCardProbability = 1.0F
            )
        )

        val newState = state.consumeTransition(prediction)
        assertSame(state, newState)
    }
}
