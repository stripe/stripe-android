package com.stripe.android.stripecardscan.payment.ml

import android.graphics.Bitmap
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripecardscan.framework.image.toMLImage
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SSDOcrWithFallbackTest {

    @Test
    @SmallTest
    fun `when primary succeeds fallback is not called`() = runScenario(
        primaryPrediction = SSDOcr.Prediction("4242424242424242")
    ) {
        val result = systemUnderTest.analyze(testInput, Unit)

        assertThat(result.pan).isEqualTo("4242424242424242")
        assertThat(fakePrimary.analyzeCalls.awaitItem()).isSameInstanceAs(testInput)
    }

    @Test
    @SmallTest
    fun `when primary returns null falls back to fallback`() = runScenario(
        primaryPrediction = SSDOcr.Prediction(null),
        fallbackPrediction = SSDOcr.Prediction("4847186095118770")
    ) {
        val result = systemUnderTest.analyze(testInput, Unit)

        assertThat(result.pan).isEqualTo("4847186095118770")
        assertThat(fakePrimary.analyzeCalls.awaitItem()).isSameInstanceAs(testInput)
        assertThat(fakeFallback.analyzeCalls.awaitItem()).isSameInstanceAs(testInput)
    }

    @Test
    @SmallTest
    fun `when primary is null uses fallback only`() = runScenario(
        includePrimary = false,
        fallbackPrediction = SSDOcr.Prediction("4847186095118770")
    ) {
        val result = systemUnderTest.analyze(testInput, Unit)

        assertThat(result.pan).isEqualTo("4847186095118770")
        assertThat(fakeFallback.analyzeCalls.awaitItem()).isSameInstanceAs(testInput)
    }

    @Test
    @SmallTest
    fun `when both return null returns null prediction`() = runScenario(
        primaryPrediction = SSDOcr.Prediction(null),
        fallbackPrediction = SSDOcr.Prediction(null)
    ) {
        val result = systemUnderTest.analyze(testInput, Unit)

        assertThat(result.pan).isNull()
        assertThat(fakePrimary.analyzeCalls.awaitItem()).isSameInstanceAs(testInput)
        assertThat(fakeFallback.analyzeCalls.awaitItem()).isSameInstanceAs(testInput)
    }

    @Test
    @SmallTest
    fun `close closes primary but not fallback`() = runScenario {
        systemUnderTest.close()

        assertThat(fakePrimary.closed).isTrue()
        assertThat(fakeFallback.closed).isFalse()
    }

    @Test
    @SmallTest
    fun `close with null primary does not throw`() = runScenario(
        includePrimary = false
    ) {
        systemUnderTest.close()

        assertThat(fakeFallback.closed).isFalse()
    }

    @Test
    @SmallTest
    fun `when primary succeeds expiry is null with non-MLKit fallback`() = runScenario(
        primaryPrediction = SSDOcr.Prediction("4242424242424242")
    ) {
        val result = systemUnderTest.analyze(testInput, Unit)

        assertThat(result.pan).isEqualTo("4242424242424242")
        assertThat(result.expiry).isNull()
        assertThat(fakePrimary.analyzeCalls.awaitItem()).isSameInstanceAs(testInput)
    }

    @Test
    @SmallTest
    fun `when fallback returns both PAN and expiry result contains both`() = runScenario(
        primaryPrediction = SSDOcr.Prediction(null),
        fallbackPrediction = SSDOcr.Prediction(
            pan = "4847186095118770",
            expiry = SSDOcr.ExpiryDate(12, 2028)
        )
    ) {
        val result = systemUnderTest.analyze(testInput, Unit)

        assertThat(result.pan).isEqualTo("4847186095118770")
        assertThat(result.expiry).isEqualTo(SSDOcr.ExpiryDate(12, 2028))
        assertThat(fakePrimary.analyzeCalls.awaitItem()).isSameInstanceAs(testInput)
        assertThat(fakeFallback.analyzeCalls.awaitItem()).isSameInstanceAs(testInput)
    }

    @Test
    @SmallTest
    fun `when no primary full fallback returns both PAN and expiry`() = runScenario(
        includePrimary = false,
        fallbackPrediction = SSDOcr.Prediction(
            pan = "4847186095118770",
            expiry = SSDOcr.ExpiryDate(3, 2029)
        )
    ) {
        val result = systemUnderTest.analyze(testInput, Unit)

        assertThat(result.pan).isEqualTo("4847186095118770")
        assertThat(result.expiry).isEqualTo(SSDOcr.ExpiryDate(3, 2029))
        assertThat(fakeFallback.analyzeCalls.awaitItem()).isSameInstanceAs(testInput)
    }

    private fun runScenario(
        primaryPrediction: SSDOcr.Prediction = SSDOcr.Prediction(null),
        fallbackPrediction: SSDOcr.Prediction = SSDOcr.Prediction(null),
        includePrimary: Boolean = true,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val fakePrimary = FakeAnalyzer(primaryPrediction)
        val fakeFallback = FakeAnalyzer(fallbackPrediction)

        val bitmap = Bitmap.createBitmap(600, 375, Bitmap.Config.ARGB_8888)
        val testInput = SSDOcr.Input(
            ssdOcrImage = bitmap.toMLImage(mean = 127.5f, std = 128.5f),
            cardBitmap = bitmap
        )

        val systemUnderTest = SSDOcrWithFallback(
            primary = if (includePrimary) fakePrimary else null,
            fallback = fakeFallback
        )

        block(
            Scenario(
                systemUnderTest = systemUnderTest,
                fakePrimary = fakePrimary,
                fakeFallback = fakeFallback,
                testInput = testInput
            )
        )

        if (includePrimary) {
            fakePrimary.ensureAllEventsConsumed()
        }
        fakeFallback.ensureAllEventsConsumed()
    }

    private class Scenario(
        val systemUnderTest: SSDOcrWithFallback,
        val fakePrimary: FakeAnalyzer,
        val fakeFallback: FakeAnalyzer,
        val testInput: SSDOcr.Input
    )
}
