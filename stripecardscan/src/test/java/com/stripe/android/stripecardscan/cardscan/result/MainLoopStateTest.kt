package com.stripe.android.stripecardscan.cardscan.result

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripecardscan.payment.ml.SSDOcr
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.TimeSource

class MainLoopStateTest {

    @Test
    @SmallTest
    fun `Initial passes expiry through to OcrFound`() = runTest {
        val state = MainLoopState.Initial(TimeSource.Monotonic)
        val expiry = SSDOcr.ExpiryDate(12, 2028)

        val next = state.consumeTransition(
            SSDOcr.Prediction(pan = "4242424242424242", expiry = expiry)
        )

        assertThat(next).isInstanceOf(MainLoopState.OcrFound::class.java)
        val ocrFound = next as MainLoopState.OcrFound
        assertThat(ocrFound.mostLikelyExpiry).isEqualTo(expiry)
    }

    @Test
    @SmallTest
    fun `Initial with null expiry creates OcrFound with null expiry`() = runTest {
        val state = MainLoopState.Initial(TimeSource.Monotonic)

        val next = state.consumeTransition(
            SSDOcr.Prediction(pan = "4242424242424242", expiry = null)
        )

        assertThat(next).isInstanceOf(MainLoopState.OcrFound::class.java)
        val ocrFound = next as MainLoopState.OcrFound
        assertThat(ocrFound.mostLikelyExpiry).isNull()
    }

    @Test
    @SmallTest
    fun `OcrFound finishes with most frequently seen expiry`() = runTest {
        val state = MainLoopState.Initial(TimeSource.Monotonic)
        val expiry1 = SSDOcr.ExpiryDate(12, 2028)
        val expiry2 = SSDOcr.ExpiryDate(3, 2029)

        // First transition creates OcrFound
        var current = state.consumeTransition(
            SSDOcr.Prediction(pan = "4242424242424242", expiry = expiry1)
        )

        // Send expiry1 two more times
        current = current.consumeTransition(
            SSDOcr.Prediction(pan = "4242424242424242", expiry = expiry1)
        )
        current = current.consumeTransition(
            SSDOcr.Prediction(pan = "4242424242424242", expiry = expiry2)
        )

        // Third PAN agreement triggers Finished
        assertThat(current).isInstanceOf(MainLoopState.Finished::class.java)
        val finished = current as MainLoopState.Finished
        assertThat(finished.pan).isEqualTo("4242424242424242")
        assertThat(finished.expiry).isEqualTo(expiry1)
    }

    @Test
    @SmallTest
    fun `OcrFound finishes with null expiry when never detected`() = runTest {
        val state = MainLoopState.Initial(TimeSource.Monotonic)

        var current = state.consumeTransition(
            SSDOcr.Prediction(pan = "4242424242424242")
        )
        current = current.consumeTransition(
            SSDOcr.Prediction(pan = "4242424242424242")
        )
        current = current.consumeTransition(
            SSDOcr.Prediction(pan = "4242424242424242")
        )

        assertThat(current).isInstanceOf(MainLoopState.Finished::class.java)
        val finished = current as MainLoopState.Finished
        assertThat(finished.pan).isEqualTo("4242424242424242")
        assertThat(finished.expiry).isNull()
    }

    @Test
    @SmallTest
    fun `OcrFound tracks expiry counts across multiple transitions`() = runTest {
        val state = MainLoopState.Initial(TimeSource.Monotonic)
        val expiry1 = SSDOcr.ExpiryDate(12, 2028)
        val expiry2 = SSDOcr.ExpiryDate(3, 2029)

        var current = state.consumeTransition(
            SSDOcr.Prediction(pan = "4242424242424242", expiry = expiry1)
        )

        // Send expiry2 twice, expiry1 only once more (1 from initial + 0 more = 1 total for expiry1)
        current = current.consumeTransition(
            SSDOcr.Prediction(pan = "4242424242424242", expiry = expiry2)
        )
        current = current.consumeTransition(
            SSDOcr.Prediction(pan = "4242424242424242", expiry = expiry2)
        )

        assertThat(current).isInstanceOf(MainLoopState.Finished::class.java)
        val finished = current as MainLoopState.Finished
        assertThat(finished.expiry).isEqualTo(expiry2)
    }

    @Test
    @SmallTest
    fun `Initial stays in Initial when PAN is null`() = runTest {
        val state = MainLoopState.Initial(TimeSource.Monotonic)

        val next = state.consumeTransition(
            SSDOcr.Prediction(pan = null, expiry = SSDOcr.ExpiryDate(12, 2028))
        )

        assertThat(next).isInstanceOf(MainLoopState.Initial::class.java)
    }
}
