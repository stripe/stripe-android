package com.stripe.android.stripecardscan.framework.ml

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripecardscan.framework.ml.ssd.rectForm
import org.junit.Test

class NonMaximumSuppressionTest {

    @Test
    fun `empty boxes returns empty result`() {
        val result = hardNonMaximumSuppression(
            boxes = emptyArray(),
            probabilities = floatArrayOf(),
            iouThreshold = 0.5f,
            limit = null,
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun `single box returns that box index`() {
        val boxes = arrayOf(rectForm(0f, 0f, 1f, 1f))
        val probabilities = floatArrayOf(0.9f)

        val result = hardNonMaximumSuppression(
            boxes = boxes,
            probabilities = probabilities,
            iouThreshold = 0.5f,
            limit = null,
        )

        assertThat(result).containsExactly(0)
    }

    @Test
    fun `two non-overlapping boxes keeps both`() {
        val boxes = arrayOf(
            rectForm(0f, 0f, 0.4f, 0.4f),
            rectForm(0.6f, 0.6f, 1f, 1f),
        )
        val probabilities = floatArrayOf(0.9f, 0.8f)

        val result = hardNonMaximumSuppression(
            boxes = boxes,
            probabilities = probabilities,
            iouThreshold = 0.5f,
            limit = null,
        )

        assertThat(result).containsExactly(0, 1)
    }

    @Test
    fun `two fully overlapping boxes keeps only higher probability`() {
        val boxes = arrayOf(
            rectForm(0f, 0f, 1f, 1f),
            rectForm(0f, 0f, 1f, 1f),
        )
        val probabilities = floatArrayOf(0.9f, 0.8f)

        val result = hardNonMaximumSuppression(
            boxes = boxes,
            probabilities = probabilities,
            iouThreshold = 0.5f,
            limit = null,
        )

        assertThat(result).containsExactly(0)
    }

    @Test
    fun `partially overlapping boxes above threshold suppresses lower`() {
        // Two boxes that overlap significantly (IOU > 0.5)
        val boxes = arrayOf(
            rectForm(0f, 0f, 0.8f, 0.8f),
            rectForm(0.1f, 0.1f, 0.9f, 0.9f),
        )
        val probabilities = floatArrayOf(0.9f, 0.8f)

        val result = hardNonMaximumSuppression(
            boxes = boxes,
            probabilities = probabilities,
            iouThreshold = 0.5f,
            limit = null,
        )

        assertThat(result).containsExactly(0)
    }

    @Test
    fun `limit parameter caps the number of returned indices`() {
        val boxes = arrayOf(
            rectForm(0f, 0f, 0.1f, 0.1f),
            rectForm(0.3f, 0.3f, 0.4f, 0.4f),
            rectForm(0.6f, 0.6f, 0.7f, 0.7f),
        )
        val probabilities = floatArrayOf(0.9f, 0.8f, 0.7f)

        val result = hardNonMaximumSuppression(
            boxes = boxes,
            probabilities = probabilities,
            iouThreshold = 0.5f,
            limit = 2,
        )

        assertThat(result).hasSize(2)
    }

    @Test
    fun `boxes are selected in order of decreasing probability`() {
        val boxes = arrayOf(
            rectForm(0f, 0f, 0.1f, 0.1f),
            rectForm(0.3f, 0.3f, 0.4f, 0.4f),
            rectForm(0.6f, 0.6f, 0.7f, 0.7f),
        )
        // Highest probability is index 2
        val probabilities = floatArrayOf(0.5f, 0.7f, 0.9f)

        val result = hardNonMaximumSuppression(
            boxes = boxes,
            probabilities = probabilities,
            iouThreshold = 0.5f,
            limit = null,
        )

        assertThat(result).containsExactly(2, 1, 0).inOrder()
    }

    @Test
    fun `multiple overlapping groups each keep their highest`() {
        val boxes = arrayOf(
            // Group 1: overlapping pair
            rectForm(0f, 0f, 0.5f, 0.5f),
            rectForm(0.05f, 0.05f, 0.55f, 0.55f),
            // Group 2: far away, non-overlapping with group 1
            rectForm(0.8f, 0.8f, 1f, 1f),
        )
        val probabilities = floatArrayOf(0.9f, 0.85f, 0.7f)

        val result = hardNonMaximumSuppression(
            boxes = boxes,
            probabilities = probabilities,
            iouThreshold = 0.5f,
            limit = null,
        )

        // Should keep index 0 (best of group 1) and index 2 (group 2)
        assertThat(result).containsExactly(0, 2)
    }
}
