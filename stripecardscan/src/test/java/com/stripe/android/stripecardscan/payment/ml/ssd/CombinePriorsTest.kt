package com.stripe.android.stripecardscan.payment.ml.ssd

import android.util.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CombinePriorsTest {

    private val trainedImageSize = Size(600, 375)

    @Test
    fun `combinePriors returns correct total count`() {
        val priors = combinePriors(trainedImageSize)

        // Layer 1: 38 * 24 * 3 = 2736, Layer 2: 19 * 12 * 3 = 684, Total = 3420
        assertThat(priors).hasLength(3420)
    }

    @Test
    fun `all prior values are clamped between 0 and 1`() {
        val priors = combinePriors(trainedImageSize)

        for (prior in priors) {
            assertThat(prior[0]).isAtLeast(0f)
            assertThat(prior[0]).isAtMost(1f)
            assertThat(prior[1]).isAtLeast(0f)
            assertThat(prior[1]).isAtMost(1f)
            assertThat(prior[2]).isAtLeast(0f)
            assertThat(prior[2]).isAtMost(1f)
            assertThat(prior[3]).isAtLeast(0f)
            assertThat(prior[3]).isAtMost(1f)
        }
    }

    @Test
    fun `priors have positive width and height`() {
        val priors = combinePriors(trainedImageSize)

        for (prior in priors) {
            // width is index 2, height is index 3 (SizeAndCenter format)
            assertThat(prior[2]).isGreaterThan(0f)
            assertThat(prior[3]).isGreaterThan(0f)
        }
    }

    @Test
    fun `combinePriors is deterministic for same input`() {
        val priors1 = combinePriors(trainedImageSize)
        val priors2 = combinePriors(trainedImageSize)

        assertThat(priors1).hasLength(priors2.size)
        for (i in priors1.indices) {
            assertThat(priors1[i]).isEqualTo(priors2[i])
        }
    }

    @Test
    fun `layer one generates 2736 priors and layer two generates 684`() {
        val priors = combinePriors(trainedImageSize)

        // Total is 3420; first 2736 from layer one, last 684 from layer two
        assertThat(priors).hasLength(2736 + 684)
    }
}
