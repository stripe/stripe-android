package com.stripe.android.stripecardscan.cardscan.result

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripecardscan.payment.ml.CardOcr
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.TestTimeSource

class MainLoopStateTest {

    private val timeSource = TestTimeSource()

    // --- Initial state ---

    @Test
    fun `Initial stays Initial when PAN is null`() = runTest {
        val state = MainLoopState.Initial(timeSource)

        val next = state.consumeTransition(CardOcr.Prediction(pan = null))

        assertThat(next).isInstanceOf(MainLoopState.Initial::class.java)
    }

    @Test
    fun `Initial stays Initial when PAN is empty`() = runTest {
        val state = MainLoopState.Initial(timeSource)

        val next = state.consumeTransition(CardOcr.Prediction(pan = ""))

        assertThat(next).isInstanceOf(MainLoopState.Initial::class.java)
    }

    @Test
    fun `Initial transitions to OcrFound on valid PAN`() = runTest {
        val state = MainLoopState.Initial(timeSource)

        val next = state.consumeTransition(CardOcr.Prediction(pan = "4242424242424242"))

        assertThat(next).isInstanceOf(MainLoopState.OcrFound::class.java)
    }

    // --- OcrFound state ---

    @Test
    fun `OcrFound reaches Finished after DESIRED_OCR_AGREEMENT matching PANs`() = runTest {
        var state: MainLoopState = MainLoopState.Initial(timeSource)

        // Frame 1: PAN found -> OcrFound (1 count)
        state = state.consumeTransition(CardOcr.Prediction(pan = "4242424242424242"))
        assertThat(state).isInstanceOf(MainLoopState.OcrFound::class.java)

        // Frame 2: same PAN -> 2 counts
        state = state.consumeTransition(CardOcr.Prediction(pan = "4242424242424242"))
        assertThat(state).isInstanceOf(MainLoopState.OcrFound::class.java)

        // Frame 3: same PAN -> 3 counts -> Finished
        state = state.consumeTransition(CardOcr.Prediction(pan = "4242424242424242"))

        assertThat(state).isInstanceOf(MainLoopState.Finished::class.java)
        assertThat((state as MainLoopState.Finished).pan).isEqualTo("4242424242424242")
    }

    @Test
    fun `OcrFound stays OcrFound when PANs disagree`() = runTest {
        var state: MainLoopState = MainLoopState.Initial(timeSource)

        // Frame 1: PAN A -> OcrFound
        state = state.consumeTransition(CardOcr.Prediction(pan = "4242424242424242"))
        assertThat(state).isInstanceOf(MainLoopState.OcrFound::class.java)

        // Frame 2: PAN B -> still OcrFound (no agreement)
        state = state.consumeTransition(CardOcr.Prediction(pan = "5500000000000004"))

        assertThat(state).isInstanceOf(MainLoopState.OcrFound::class.java)
    }

    @Test
    fun `OcrFound stays OcrFound on null PAN frames`() = runTest {
        var state: MainLoopState = MainLoopState.Initial(timeSource)

        state = state.consumeTransition(CardOcr.Prediction(pan = "4242424242424242"))
        assertThat(state).isInstanceOf(MainLoopState.OcrFound::class.java)

        // Null PAN frame doesn't reset or advance
        state = state.consumeTransition(CardOcr.Prediction(pan = null))

        assertThat(state).isInstanceOf(MainLoopState.OcrFound::class.java)
    }

    // --- Expiry accumulation ---

    @Test
    fun `Initial forwards expiry to OcrFound`() = runTest {
        val state = MainLoopState.Initial(timeSource)

        var next = state.consumeTransition(
            CardOcr.Prediction(pan = "4242424242424242", expiryMonth = 12, expiryYear = 2028)
        )
        assertThat(next).isInstanceOf(MainLoopState.OcrFound::class.java)

        // Send 2 more frames to reach Finished
        next = next.consumeTransition(CardOcr.Prediction(pan = "4242424242424242"))
        next = next.consumeTransition(CardOcr.Prediction(pan = "4242424242424242"))

        assertThat(next).isInstanceOf(MainLoopState.Finished::class.java)
        val finished = next as MainLoopState.Finished
        assertThat(finished.expiryMonth).isEqualTo(12)
        assertThat(finished.expiryYear).isEqualTo(2028)
    }

    @Test
    fun `OcrFound carries expiry to Finished`() = runTest {
        var state: MainLoopState = MainLoopState.Initial(timeSource)
        val prediction = CardOcr.Prediction(
            pan = "4242424242424242",
            expiryMonth = 6,
            expiryYear = 2029,
        )

        // 3 frames with same PAN and expiry -> Finished
        state = state.consumeTransition(prediction)
        state = state.consumeTransition(prediction)
        state = state.consumeTransition(prediction)

        assertThat(state).isInstanceOf(MainLoopState.Finished::class.java)
        val finished = state as MainLoopState.Finished
        assertThat(finished.pan).isEqualTo("4242424242424242")
        assertThat(finished.expiryMonth).isEqualTo(6)
        assertThat(finished.expiryYear).isEqualTo(2029)
    }

    @Test
    fun `OcrFound uses most frequent expiry`() = runTest {
        var state: MainLoopState = MainLoopState.Initial(timeSource)

        // Frame 1: expiry A
        state = state.consumeTransition(
            CardOcr.Prediction(pan = "4242424242424242", expiryMonth = 1, expiryYear = 2027)
        )
        // Frame 2: expiry B (different)
        state = state.consumeTransition(
            CardOcr.Prediction(pan = "4242424242424242", expiryMonth = 6, expiryYear = 2029)
        )
        // Frame 3: expiry A again (A wins with 2 vs 1)
        state = state.consumeTransition(
            CardOcr.Prediction(pan = "4242424242424242", expiryMonth = 1, expiryYear = 2027)
        )

        assertThat(state).isInstanceOf(MainLoopState.Finished::class.java)
        val finished = state as MainLoopState.Finished
        assertThat(finished.expiryMonth).isEqualTo(1)
        assertThat(finished.expiryYear).isEqualTo(2027)
    }

    @Test
    fun `OcrFound handles null expiry gracefully`() = runTest {
        var state: MainLoopState = MainLoopState.Initial(timeSource)

        // 3 frames with PAN but no expiry
        state = state.consumeTransition(CardOcr.Prediction(pan = "4242424242424242"))
        state = state.consumeTransition(CardOcr.Prediction(pan = "4242424242424242"))
        state = state.consumeTransition(CardOcr.Prediction(pan = "4242424242424242"))

        assertThat(state).isInstanceOf(MainLoopState.Finished::class.java)
        val finished = state as MainLoopState.Finished
        assertThat(finished.expiryMonth).isNull()
        assertThat(finished.expiryYear).isNull()
    }

    @Test
    fun `OcrFound accumulates expiry arriving on later frames`() = runTest {
        var state: MainLoopState = MainLoopState.Initial(timeSource)

        // Frame 1: PAN only, no expiry
        state = state.consumeTransition(CardOcr.Prediction(pan = "4242424242424242"))
        // Frame 2: PAN + expiry
        state = state.consumeTransition(
            CardOcr.Prediction(pan = "4242424242424242", expiryMonth = 3, expiryYear = 2030)
        )
        // Frame 3: PAN -> Finished
        state = state.consumeTransition(CardOcr.Prediction(pan = "4242424242424242"))

        assertThat(state).isInstanceOf(MainLoopState.Finished::class.java)
        val finished = state as MainLoopState.Finished
        assertThat(finished.expiryMonth).isEqualTo(3)
        assertThat(finished.expiryYear).isEqualTo(2030)
    }
}
