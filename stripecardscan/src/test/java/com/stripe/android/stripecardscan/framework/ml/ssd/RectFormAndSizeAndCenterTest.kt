package com.stripe.android.stripecardscan.framework.ml.ssd

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.exp

class RectFormAndSizeAndCenterTest {

    // --- RectForm ---

    @Test
    fun `rectForm constructor sets left top right bottom correctly`() {
        val rect = rectForm(0.1f, 0.2f, 0.8f, 0.9f)

        assertThat(rect.left()).isEqualTo(0.1f)
        assertThat(rect.top()).isEqualTo(0.2f)
        assertThat(rect.right()).isEqualTo(0.8f)
        assertThat(rect.bottom()).isEqualTo(0.9f)
    }

    @Test
    fun `calcWidth returns right minus left`() {
        val rect = rectForm(0.1f, 0f, 0.9f, 1f)

        assertThat(rect.calcWidth()).isWithin(FLOAT_TOLERANCE).of(0.8f)
    }

    @Test
    fun `calcHeight returns bottom minus top`() {
        val rect = rectForm(0f, 0.2f, 1f, 0.7f)

        assertThat(rect.calcHeight()).isWithin(FLOAT_TOLERANCE).of(0.5f)
    }

    @Test
    fun `areaClamped returns correct area for normal rect`() {
        val rect = rectForm(0f, 0f, 2f, 3f)

        assertThat(rect.areaClamped()).isWithin(FLOAT_TOLERANCE).of(6f)
    }

    @Test
    fun `areaClamped clamps negative dimensions to zero`() {
        // right < left, so width is negative -> clamped to 0
        val rect = rectForm(0.5f, 0f, 0.2f, 1f)

        assertThat(rect.areaClamped()).isEqualTo(0f)
    }

    @Test
    fun `overlapWith returns intersection of two overlapping rects`() {
        val rect1 = rectForm(0f, 0f, 0.6f, 0.6f)
        val rect2 = rectForm(0.4f, 0.4f, 1f, 1f)

        val overlap = rect1.overlapWith(rect2)

        assertThat(overlap.left()).isEqualTo(0.4f)
        assertThat(overlap.top()).isEqualTo(0.4f)
        assertThat(overlap.right()).isEqualTo(0.6f)
        assertThat(overlap.bottom()).isEqualTo(0.6f)
    }

    @Test
    fun `overlapWith returns negative-area rect for non-overlapping rects`() {
        val rect1 = rectForm(0f, 0f, 0.3f, 0.3f)
        val rect2 = rectForm(0.5f, 0.5f, 1f, 1f)

        val overlap = rect1.overlapWith(rect2)

        // right < left and bottom < top, so area is negative before clamping
        assertThat(overlap.areaClamped()).isEqualTo(0f)
    }

    @Test
    fun `setters mutate values in place`() {
        val rect = rectForm(0f, 0f, 0f, 0f)

        rect.setLeft(0.1f)
        rect.setTop(0.2f)
        rect.setRight(0.3f)
        rect.setBottom(0.4f)

        assertThat(rect.left()).isEqualTo(0.1f)
        assertThat(rect.top()).isEqualTo(0.2f)
        assertThat(rect.right()).isEqualTo(0.3f)
        assertThat(rect.bottom()).isEqualTo(0.4f)
    }

    // --- SizeAndCenter ---

    @Test
    fun `sizeAndCenter constructor sets fields correctly`() {
        val sc = sizeAndCenter(0.5f, 0.5f, 0.4f, 0.6f)

        assertThat(sc.centerX()).isEqualTo(0.5f)
        assertThat(sc.centerY()).isEqualTo(0.5f)
        assertThat(sc.width()).isEqualTo(0.4f)
        assertThat(sc.height()).isEqualTo(0.6f)
    }

    @Test
    fun `toRectForm converts center and size to left top right bottom`() {
        val sc = sizeAndCenter(0.5f, 0.5f, 0.4f, 0.6f)

        sc.toRectForm()

        // left = 0.5 - 0.4/2 = 0.3, top = 0.5 - 0.6/2 = 0.2
        // right = 0.5 + 0.4/2 = 0.7, bottom = 0.5 + 0.6/2 = 0.8
        assertThat(sc[0]).isWithin(FLOAT_TOLERANCE).of(0.3f)
        assertThat(sc[1]).isWithin(FLOAT_TOLERANCE).of(0.2f)
        assertThat(sc[2]).isWithin(FLOAT_TOLERANCE).of(0.7f)
        assertThat(sc[3]).isWithin(FLOAT_TOLERANCE).of(0.8f)
    }

    @Test
    fun `clampAll clamps values within range`() {
        val sc = sizeAndCenter(-0.1f, 1.5f, 0.5f, 0.3f)

        sc.clampAll(0f, 1f)

        assertThat(sc.centerX()).isEqualTo(0f)
        assertThat(sc.centerY()).isEqualTo(1f)
        assertThat(sc.width()).isEqualTo(0.5f)
        assertThat(sc.height()).isEqualTo(0.3f)
    }

    @Test
    fun `clampAll does not modify values already within range`() {
        val sc = sizeAndCenter(0.3f, 0.4f, 0.5f, 0.6f)

        sc.clampAll(0f, 1f)

        assertThat(sc.centerX()).isEqualTo(0.3f)
        assertThat(sc.centerY()).isEqualTo(0.4f)
        assertThat(sc.width()).isEqualTo(0.5f)
        assertThat(sc.height()).isEqualTo(0.6f)
    }

    @Test
    fun `adjustLocation applies center and size variance with prior`() {
        val location = sizeAndCenter(1f, 1f, 0f, 0f)
        val prior = sizeAndCenter(0.5f, 0.5f, 0.2f, 0.3f)
        val centerVariance = 0.1f
        val sizeVariance = 0.2f

        location.adjustLocation(prior, centerVariance, sizeVariance)

        // centerX = 1.0 * 0.1 * 0.2 + 0.5 = 0.52
        assertThat(location.centerX()).isWithin(FLOAT_TOLERANCE).of(0.52f)
        // centerY = 1.0 * 0.1 * 0.3 + 0.5 = 0.53
        assertThat(location.centerY()).isWithin(FLOAT_TOLERANCE).of(0.53f)
        // width = exp(0.0 * 0.2) * 0.2 = 1.0 * 0.2 = 0.2
        assertThat(location.width()).isWithin(FLOAT_TOLERANCE).of(0.2f)
        // height = exp(0.0 * 0.2) * 0.3 = 1.0 * 0.3 = 0.3
        assertThat(location.height()).isWithin(FLOAT_TOLERANCE).of(0.3f)
    }

    @Test
    fun `adjustLocations applies to all elements`() {
        val locations = arrayOf(
            sizeAndCenter(0f, 0f, 0f, 0f),
            sizeAndCenter(0f, 0f, 0f, 0f),
        )
        val priors = arrayOf(
            sizeAndCenter(0.3f, 0.3f, 0.1f, 0.1f),
            sizeAndCenter(0.7f, 0.7f, 0.2f, 0.2f),
        )

        locations.adjustLocations(priors, centerVariance = 0.1f, sizeVariance = 0.2f)

        // Both should have been adjusted: centerX = 0 * 0.1 * width + priorCenterX = priorCenterX
        assertThat(locations[0].centerX()).isWithin(FLOAT_TOLERANCE).of(0.3f)
        assertThat(locations[1].centerX()).isWithin(FLOAT_TOLERANCE).of(0.7f)
        // width = exp(0) * priorWidth = priorWidth
        assertThat(locations[0].width()).isWithin(FLOAT_TOLERANCE).of(0.1f)
        assertThat(locations[1].width()).isWithin(FLOAT_TOLERANCE).of(0.2f)
    }

    @Test
    fun `adjustLocation with non-zero size input scales exponentially`() {
        val location = sizeAndCenter(0f, 0f, 1f, 1f)
        val prior = sizeAndCenter(0.5f, 0.5f, 0.1f, 0.1f)
        val sizeVariance = 0.2f

        location.adjustLocation(prior, centerVariance = 0.1f, sizeVariance = sizeVariance)

        // width = exp(1.0 * 0.2) * 0.1
        val expectedWidth = exp(1f * sizeVariance) * 0.1f
        assertThat(location.width()).isWithin(FLOAT_TOLERANCE).of(expectedWidth)
    }

    companion object {
        private const val FLOAT_TOLERANCE = 0.0001f
    }
}
