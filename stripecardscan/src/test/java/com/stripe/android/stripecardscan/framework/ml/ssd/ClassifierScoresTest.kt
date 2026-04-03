package com.stripe.android.stripecardscan.framework.ml.ssd

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ClassifierScoresTest {

    @Test
    fun `softMax normalizes values to sum to 1`() {
        val scores: ClassifierScores = floatArrayOf(1f, 2f, 3f)

        scores.softMax()

        val sum = scores.sum()
        assertThat(sum).isWithin(FLOAT_TOLERANCE).of(1f)
    }

    @Test
    fun `softMax of single element returns 1`() {
        val scores: ClassifierScores = floatArrayOf(5f)

        scores.softMax()

        assertThat(scores[0]).isWithin(FLOAT_TOLERANCE).of(1f)
    }

    @Test
    fun `softMax of equal values returns equal probabilities`() {
        val scores: ClassifierScores = floatArrayOf(2f, 2f, 2f, 2f)

        scores.softMax()

        for (score in scores) {
            assertThat(score).isWithin(FLOAT_TOLERANCE).of(0.25f)
        }
    }

    @Test
    fun `softMax with large negative values does not produce NaN`() {
        val scores: ClassifierScores = floatArrayOf(-100f, -200f, -300f)

        scores.softMax()

        for (score in scores) {
            assertThat(score.isNaN()).isFalse()
            assertThat(score.isInfinite()).isFalse()
        }
        assertThat(scores.sum()).isWithin(FLOAT_TOLERANCE).of(1f)
    }

    companion object {
        private const val FLOAT_TOLERANCE = 0.0001f
    }
}
