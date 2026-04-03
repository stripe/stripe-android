package com.stripe.android.stripecardscan.cardscan.result

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.stripecardscan.framework.image.MLImage
import com.stripe.android.stripecardscan.payment.ml.CardOcr
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainLoopAggregatorTest {

    private fun createTestInput(): CardOcr.Input {
        val bitmap = Bitmap.createBitmap(600, 375, Bitmap.Config.ARGB_8888)
        val mlImage = MLImage(bitmap)
        return CardOcr.Input(ssdOcrImage = mlImage, cardBitmap = bitmap)
    }

    @Test
    fun `initial state is Initial`() {
        val listener = RecordingAggregateResultListener()
        val aggregator = MainLoopAggregator(listener)

        assertThat(aggregator.state).isInstanceOf(MainLoopState.Initial::class.java)
    }

    @Test
    fun `aggregateResult returns InterimResult with Initial state for null PAN`() = runTest {
        val listener = RecordingAggregateResultListener()
        val aggregator = MainLoopAggregator(listener)
        val input = createTestInput()

        val (interim, final_) = aggregator.aggregateResult(input, CardOcr.Prediction(pan = null))

        assertThat(interim.state).isInstanceOf(MainLoopState.Initial::class.java)
        assertThat(interim.analyzerResult.pan).isNull()
        assertThat(final_).isNull()
    }

    @Test
    fun `aggregateResult transitions to OcrFound for valid PAN`() = runTest {
        val listener = RecordingAggregateResultListener()
        val aggregator = MainLoopAggregator(listener)
        val input = createTestInput()

        val (interim, final_) = aggregator.aggregateResult(
            input,
            CardOcr.Prediction(pan = "4242424242424242")
        )

        assertThat(interim.state).isInstanceOf(MainLoopState.OcrFound::class.java)
        assertThat(final_).isNull()
    }

    @Test
    fun `aggregateResult returns FinalResult after 3 matching PANs`() = runTest {
        val listener = RecordingAggregateResultListener()
        val aggregator = MainLoopAggregator(listener)
        val input = createTestInput()
        val prediction = CardOcr.Prediction(pan = "4242424242424242")

        aggregator.aggregateResult(input, prediction)
        aggregator.aggregateResult(input, prediction)
        val (interim, final_) = aggregator.aggregateResult(input, prediction)

        assertThat(interim.state).isInstanceOf(MainLoopState.Finished::class.java)
        assertThat(final_).isNotNull()
        assertThat(final_!!.pan).isEqualTo("4242424242424242")
    }

    @Test
    fun `aggregateResult returns null FinalResult until agreement reached`() = runTest {
        val listener = RecordingAggregateResultListener()
        val aggregator = MainLoopAggregator(listener)
        val input = createTestInput()

        val (_, final1) = aggregator.aggregateResult(
            input, CardOcr.Prediction(pan = "4242424242424242")
        )
        val (_, final2) = aggregator.aggregateResult(
            input, CardOcr.Prediction(pan = "4242424242424242")
        )

        assertThat(final1).isNull()
        assertThat(final2).isNull()
    }

    @Test
    fun `aggregateResult carries expiry through to FinalResult`() = runTest {
        val listener = RecordingAggregateResultListener()
        val aggregator = MainLoopAggregator(listener)
        val input = createTestInput()
        val prediction = CardOcr.Prediction(
            pan = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2028,
        )

        aggregator.aggregateResult(input, prediction)
        aggregator.aggregateResult(input, prediction)
        val (_, final_) = aggregator.aggregateResult(input, prediction)

        assertThat(final_).isNotNull()
        assertThat(final_!!.expiryMonth).isEqualTo(12)
        assertThat(final_.expiryYear).isEqualTo(2028)
    }

    @Test
    fun `aggregateResult handles empty PAN as no-PAN`() = runTest {
        val listener = RecordingAggregateResultListener()
        val aggregator = MainLoopAggregator(listener)
        val input = createTestInput()

        val (interim, final_) = aggregator.aggregateResult(
            input, CardOcr.Prediction(pan = "")
        )

        assertThat(interim.state).isInstanceOf(MainLoopState.Initial::class.java)
        assertThat(final_).isNull()
    }

    @Test
    fun `state is updated after each aggregateResult call`() = runTest {
        val listener = RecordingAggregateResultListener()
        val aggregator = MainLoopAggregator(listener)
        val input = createTestInput()

        assertThat(aggregator.state).isInstanceOf(MainLoopState.Initial::class.java)

        aggregator.aggregateResult(input, CardOcr.Prediction(pan = "4242424242424242"))
        assertThat(aggregator.state).isInstanceOf(MainLoopState.OcrFound::class.java)
    }

    @Test
    fun `disagreeing PANs do not reach Finished`() = runTest {
        val listener = RecordingAggregateResultListener()
        val aggregator = MainLoopAggregator(listener)
        val input = createTestInput()

        aggregator.aggregateResult(input, CardOcr.Prediction(pan = "4242424242424242"))
        aggregator.aggregateResult(input, CardOcr.Prediction(pan = "5500000000000004"))
        val (interim, final_) = aggregator.aggregateResult(
            input, CardOcr.Prediction(pan = "378282246310005")
        )

        assertThat(interim.state).isInstanceOf(MainLoopState.OcrFound::class.java)
        assertThat(final_).isNull()
    }

    /**
     * Simple recording listener for testing. We only need to verify the aggregator's return values,
     * not the listener callbacks (those are tested via ResultAggregator in camera-core).
     */
    private class RecordingAggregateResultListener :
        AggregateResultListener<MainLoopAggregator.InterimResult, MainLoopAggregator.FinalResult> {

        override suspend fun onResult(result: MainLoopAggregator.FinalResult) = Unit
        override suspend fun onInterimResult(result: MainLoopAggregator.InterimResult) = Unit
        override suspend fun onReset() = Unit
    }
}
