package com.stripe.android.cardverificationsheet.cardverifyui.result

import androidx.test.filters.LargeTest
import com.stripe.android.cardverificationsheet.cardverifyui.VerifyConfig
import com.stripe.android.cardverificationsheet.cardverifyui.analyzer.MainLoopAnalyzer
import com.stripe.android.cardverificationsheet.framework.time.delay
import com.stripe.android.cardverificationsheet.framework.time.milliseconds
import com.stripe.android.cardverificationsheet.framework.util.ItemCounter
import com.stripe.android.cardverificationsheet.payment.card.CardIssuer
import com.stripe.android.cardverificationsheet.payment.ml.CardDetect
import com.stripe.android.cardverificationsheet.payment.ml.SSDOcr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MainLoopStateMachineTest {

    @Test
    fun initial_runsOcrOnly() {
        val state = MainLoopState.Initial(
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        assertTrue(state.runOcr)
        assertFalse(state.runCardDetect)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun initial_noCard_noOcr() = runBlockingTest {
        val state = MainLoopState.Initial(
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = null,
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.Initial)
        assertEquals(CardIssuer.Visa, newState.requiredCardIssuer)
        assertEquals("8770", newState.requiredLastFour)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun initial_wrongCard() = runBlockingTest {
        val state = MainLoopState.Initial(
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("5445435282861343"),
            card = null,
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.WrongCard)
        assertEquals("8770", newState.requiredLastFour)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun initial_noCard_foundOcr() = runBlockingTest {
        val state = MainLoopState.Initial(
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null,
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.OcrFound)
    }

    @Test
    fun panFound_runsCardDetectAndOcrOnly() {
        val state = MainLoopState.OcrFound(
            pan = "4847186095118770",
            isCardVisible = true,
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        assertTrue(state.runOcr)
        assertTrue(state.runCardDetect)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun panFound_noCard_noTimeout() = runBlockingTest {
        val state = MainLoopState.OcrFound(
            pan = "4847186095118770",
            isCardVisible = true,
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null,
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.OcrFound)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun panFound_cardSatisfied_noTimeout() = runBlockingTest {
        var state: MainLoopState = MainLoopState.OcrFound(
            pan = "4847186095118770",
            isCardVisible = true,
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = CardDetect.Prediction(
                side = CardDetect.Prediction.Side.PAN,
                panProbability = 1.0F,
                noPanProbability = 0.0F,
                noCardProbability = 0.0F,
            ),
        )

        repeat(VerifyConfig.DESIRED_CARD_COUNT - 2) {
            state = state.consumeTransition(prediction)
            assertTrue(state is MainLoopState.OcrFound)
        }

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.CardSatisfied)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun panFound_panSatisfied_noTimeout() = runBlockingTest {
        var state: MainLoopState = MainLoopState.OcrFound(
            pan = "4847186095118770",
            isCardVisible = true,
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null,
        )

        repeat(VerifyConfig.DESIRED_OCR_AGREEMENT - 2) {
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
    fun panFound_finished_timeout() = runBlocking {
        var state: MainLoopState = MainLoopState.OcrFound(
            pan = "4847186095118770",
            isCardVisible = true,
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null,
        )

        repeat(VerifyConfig.DESIRED_OCR_AGREEMENT - 3) {
            state = state.consumeTransition(prediction)
            assertTrue(state is MainLoopState.OcrFound)
        }

        delay(VerifyConfig.OCR_AND_CARD_SEARCH_DURATION + 1.milliseconds)

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.Finished)
    }

    /**
     * This test cannot use `runBlockingTest` because it requires a delay. While runBlockingTest
     * advances the dispatcher's virtual time by the specified amount, it does not affect the timing
     * of the duration.
     */
    @Test
    @ExperimentalCoroutinesApi
    fun panFound_noCardVisible_timeout() = runBlocking {
        var state: MainLoopState = MainLoopState.OcrFound(
            pan = "4847186095118770",
            isCardVisible = true,
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val predictionWithCard = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null,
        )

        repeat(VerifyConfig.DESIRED_OCR_AGREEMENT - 2) {
            state = state.consumeTransition(predictionWithCard)
            assertTrue(state is MainLoopState.OcrFound)
        }

        delay(VerifyConfig.NO_CARD_VISIBLE_DURATION + 1.milliseconds)

        val predictionWithoutCard = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = CardDetect.Prediction(CardDetect.Prediction.Side.NO_CARD, 1.0F, 0.0F, 0.0F)
        )

        val newState = state.consumeTransition(predictionWithoutCard)
        assertTrue(newState is MainLoopState.Initial)
    }

    /**
     * This test cannot use `runBlockingTest` because it requires a delay. While runBlockingTest
     * advances the dispatcher's virtual time by the specified amount, it does not affect the timing
     * of the duration.
     */
    @Test
    @LargeTest
    fun panFound_timeout() = runBlocking {
        val state = MainLoopState.OcrFound(
            pan = "4847186095118770",
            isCardVisible = true,
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        delay(VerifyConfig.OCR_AND_CARD_SEARCH_DURATION + 1.milliseconds)

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = null,
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.Finished, "$newState is not Finished")
    }

    @Test
    fun panSatisfied_runsCardDetectOnly() {
        val state = MainLoopState.OcrSatisfied(
            pan = "4847186095118770",
            visibleCardCount = 0,
        )

        assertFalse(state.runOcr)
        assertTrue(state.runCardDetect)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun panSatisfied_noCard_noTimeout() = runBlockingTest {
        val state = MainLoopState.OcrSatisfied(
            pan = "4847186095118770",
            visibleCardCount = 0,
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = CardDetect.Prediction(
                side = CardDetect.Prediction.Side.PAN,
                panProbability = 1.0F,
                noPanProbability = 0.0F,
                noCardProbability = 0.0F,
            ),
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.OcrSatisfied)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun panSatisfied_enoughSides_noTimeout() = runBlockingTest {
        val state = MainLoopState.OcrSatisfied(
            pan = "4847186095118770",
            visibleCardCount = VerifyConfig.DESIRED_CARD_COUNT - 1,
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = CardDetect.Prediction(
                side = CardDetect.Prediction.Side.PAN,
                panProbability = 1.0F,
                noPanProbability = 0.0F,
                noCardProbability = 0.0F,
            ),
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
    fun panSatisfied_timeout() = runBlocking {
        val state = MainLoopState.OcrSatisfied(
            pan = "4847186095118770",
            visibleCardCount = VerifyConfig.DESIRED_CARD_COUNT - 1,
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = null,
        )

        delay(VerifyConfig.NO_CARD_VISIBLE_DURATION + 1.milliseconds)

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.Finished)
    }

    @Test
    fun cardSatisfied_runsOcrOnly() {
        val state = MainLoopState.CardSatisfied(
            panCounter = ItemCounter("4847186095118770"),
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        assertTrue(state.runOcr)
        assertFalse(state.runCardDetect)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun cardSatisfied_noPan_noTimeout() = runBlockingTest {
        val state = MainLoopState.CardSatisfied(
            panCounter = ItemCounter("4847186095118770"),
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null,
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.CardSatisfied)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun cardSatisfied_pan_noTimeout() = runBlockingTest {
        var state: MainLoopState = MainLoopState.CardSatisfied(
            panCounter = ItemCounter("4847186095118770"),
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null,
        )

        repeat(VerifyConfig.DESIRED_OCR_AGREEMENT - 2) {
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
    fun cardSatisfied_noPan_timeout() = runBlocking {
        val state = MainLoopState.CardSatisfied(
            panCounter = ItemCounter("4847186095118770"),
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = null,
        )

        delay(VerifyConfig.OCR_ONLY_SEARCH_DURATION + 1.milliseconds)

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.Finished)
    }

    @Test
    fun wrongPanFound_runsOcrOnly() {
        val state = MainLoopState.WrongCard(
            pan = "5445435282861343",
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        assertTrue(state.runOcr)
        assertFalse(state.runCardDetect)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun wrongPanFound_noPan_noTimeout() = runBlockingTest {
        val state = MainLoopState.WrongCard(
            pan = "5445435282861343",
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = null,
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.WrongCard)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun wrongPanFound_wrongPan_noTimeout() = runBlockingTest {
        val state = MainLoopState.WrongCard(
            pan = "5445435282861343",
            requiredCardIssuer = CardIssuer.MasterCard,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("5445435282861343"),
            card = null,
        )

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.WrongCard)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun wrongPanFound_rightPan_noTimeout() = runBlockingTest {
        val state = MainLoopState.WrongCard(
            pan = "5445435282861343",
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = null,
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
    fun wrongPanFound_noPan_timeout() = runBlocking {
        val state = MainLoopState.WrongCard(
            pan = "5445435282861343",
            requiredCardIssuer = CardIssuer.Visa,
            requiredLastFour = "8770",
        )

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = null,
            card = null,
        )

        delay(VerifyConfig.WRONG_CARD_DURATION + 1.milliseconds)

        val newState = state.consumeTransition(prediction)
        assertTrue(newState is MainLoopState.Initial)
    }

    @Test
    fun finished_runsNothing() {
        val state = MainLoopState.Finished("4847186095118770")

        assertFalse(state.runOcr)
        assertFalse(state.runCardDetect)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun finished_goesNowhere() = runBlockingTest {
        val state = MainLoopState.Finished("4847186095118770")

        val prediction = MainLoopAnalyzer.Prediction(
            ocr = SSDOcr.Prediction("4847186095118770"),
            card = CardDetect.Prediction(
                side = CardDetect.Prediction.Side.NO_CARD,
                panProbability = 0.0F,
                noPanProbability = 0.0F,
                noCardProbability = 1.0F,
            ),
        )

        val newState = state.consumeTransition(prediction)
        assertSame(state, newState)
    }
}
