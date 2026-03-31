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
}
