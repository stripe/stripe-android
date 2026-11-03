package com.stripe.android.stripecardscan.payment.ml

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripecardscan.framework.ml.ssd.softMax
import com.stripe.android.stripecardscan.payment.ml.ssd.OcrFeatureMapSizes
import com.stripe.android.stripecardscan.payment.ml.ssd.rearrangeOCRArray
import org.junit.Test

class CompactModelDecoderTest {
    @Test
    fun `softmax preserves probabilities for logits that would overflow exp`() {
        val scores = floatArrayOf(1000f, 1000f, 1000f)

        scores.softMax()

        scores.forEach { assertThat(it).isWithin(0.000001f).of(1f / 3f) }
        assertThat(scores.sum()).isWithin(0.000001f).of(1f)
    }

    @Test
    fun `single grid restores channel and height order without a coarse output`() {
        // Two columns, two rows, one prior with two values: W,C,H storage.
        val output = arrayOf(floatArrayOf(10f, 20f, 11f, 21f, 30f, 40f, 31f, 41f))

        val decoded = rearrangeOCRArray(
            locations = output,
            featureMapSizes = OcrFeatureMapSizes(2, 2, 0, 0),
            numberOfPriors = 1,
            locationsPerPrior = 2,
        )

        assertThat(decoded.single().toList())
            .containsExactly(10f, 11f, 30f, 31f, 20f, 21f, 40f, 41f).inOrder()
    }
}
