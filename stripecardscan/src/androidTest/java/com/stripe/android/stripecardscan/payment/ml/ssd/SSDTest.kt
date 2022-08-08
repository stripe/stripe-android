package com.stripe.android.stripecardscan.payment.ml.ssd

import android.graphics.RectF
import com.stripe.android.stripecardscan.payment.ml.VERTICAL_THRESHOLD
import org.junit.Test
import kotlin.test.assertEquals

class SSDTest {

    @Test
    fun determineLayoutAndFilter_emptyBoxes() {
        determineLayoutAndFilter(emptyList(), VERTICAL_THRESHOLD)
    }

    @Test
    fun determineLayoutAndFilter_linearNumbers() {
        val numbers = listOf(
            DetectionBox(
                rect = RectF(0.00F, 0.00F, 0.00F, 0.00F),
                confidence = 1F,
                label = 0
            ),
            DetectionBox(
                rect = RectF(0.01F, 0.00F, 0.01F, 0.00F),
                confidence = 1F,
                label = 1
            ),
            DetectionBox(
                rect = RectF(0.02F, 0.00F, 0.02F, 0.00F),
                confidence = 1F,
                label = 2
            ),
            DetectionBox(
                rect = RectF(0.03F, 0.00F, 0.03F, 0.00F),
                confidence = 1F,
                label = 3
            ),
            DetectionBox(
                rect = RectF(0.04F, 0.00F, 0.04F, 0.00F),
                confidence = 1F,
                label = 4
            ),
            DetectionBox(
                rect = RectF(0.05F, 0.00F, 0.05F, 0.00F),
                confidence = 1F,
                label = 5
            ),
            DetectionBox(
                rect = RectF(0.06F, 0.00F, 0.06F, 0.00F),
                confidence = 1F,
                label = 6
            ),
            DetectionBox(
                rect = RectF(0.07F, 0.00F, 0.07F, 0.00F),
                confidence = 1F,
                label = 7
            ),
            DetectionBox(
                rect = RectF(0.08F, 0.00F, 0.08F, 0.00F),
                confidence = 1F,
                label = 8
            ),
            DetectionBox(
                rect = RectF(0.09F, 0.00F, 0.09F, 0.00F),
                confidence = 1F,
                label = 9
            ),
            DetectionBox(
                rect = RectF(0.10F, 0.00F, 0.10F, 0.00F),
                confidence = 1F,
                label = 0
            ),
            DetectionBox(
                rect = RectF(0.11F, 0.00F, 0.11F, 0.00F),
                confidence = 1F,
                label = 1
            ),
            DetectionBox(
                rect = RectF(0.12F, 0.00F, 0.12F, 0.00F),
                confidence = 1F,
                label = 2
            ),
            DetectionBox(
                rect = RectF(0.13F, 0.00F, 0.13F, 0.00F),
                confidence = 1F,
                label = 3
            ),
            DetectionBox(
                rect = RectF(0.14F, 0.00F, 0.14F, 0.00F),
                confidence = 1F,
                label = 4
            ),
            DetectionBox(
                rect = RectF(0.15F, 0.00F, 0.15F, 0.00F),
                confidence = 1F,
                label = 5
            )
        )

        assertEquals(
            "0123456789012345",
            determineLayoutAndFilter(numbers, VERTICAL_THRESHOLD)
                .map { it.label }
                .joinToString("")
        )
    }

    @Test
    fun determineLayoutAndFilter_visaQuickReadNumbers() {
        val numbers = listOf(
            DetectionBox(
                rect = RectF(0.00F, 0.00F, 0.00F, 0.00F),
                confidence = 1F,
                label = 0
            ),
            DetectionBox(
                rect = RectF(0.01F, 0.00F, 0.01F, 0.00F),
                confidence = 1F,
                label = 1
            ),
            DetectionBox(
                rect = RectF(0.02F, 0.00F, 0.02F, 0.00F),
                confidence = 1F,
                label = 2
            ),
            DetectionBox(
                rect = RectF(0.03F, 0.00F, 0.03F, 0.00F),
                confidence = 1F,
                label = 3
            ),
            DetectionBox(
                rect = RectF(0.00F, 0.25F, 0.00F, 0.25F),
                confidence = 1F,
                label = 4
            ),
            DetectionBox(
                rect = RectF(0.01F, 0.25F, 0.01F, 0.25F),
                confidence = 1F,
                label = 5
            ),
            DetectionBox(
                rect = RectF(0.02F, 0.25F, 0.02F, 0.25F),
                confidence = 1F,
                label = 6
            ),
            DetectionBox(
                rect = RectF(0.03F, 0.25F, 0.04F, 0.25F),
                confidence = 1F,
                label = 7
            ),
            DetectionBox(
                rect = RectF(0.00F, 0.50F, 0.00F, 0.50F),
                confidence = 1F,
                label = 8
            ),
            DetectionBox(
                rect = RectF(0.01F, 0.50F, 0.01F, 0.50F),
                confidence = 1F,
                label = 9
            ),
            DetectionBox(
                rect = RectF(0.02F, 0.50F, 0.02F, 0.50F),
                confidence = 1F,
                label = 0
            ),
            DetectionBox(
                rect = RectF(0.03F, 0.50F, 0.03F, 0.50F),
                confidence = 1F,
                label = 1
            ),
            DetectionBox(
                rect = RectF(0.00F, 0.75F, 0.00F, 0.75F),
                confidence = 1F,
                label = 2
            ),
            DetectionBox(
                rect = RectF(0.01F, 0.75F, 0.01F, 0.75F),
                confidence = 1F,
                label = 3
            ),
            DetectionBox(
                rect = RectF(0.02F, 0.75F, 0.02F, 0.75F),
                confidence = 1F,
                label = 4
            ),
            DetectionBox(
                rect = RectF(0.03F, 0.75F, 0.03F, 0.75F),
                confidence = 1F,
                label = 5
            )
        )

        assertEquals(
            "0123456789012345",
            determineLayoutAndFilter(numbers, VERTICAL_THRESHOLD)
                .map { it.label }
                .joinToString("")
        )
    }

    @Test
    fun determineLayoutAndFilter_linearDiagonalTopLeftBottomRight() {
        val numbers = listOf(
            DetectionBox(
                rect = RectF(0.00F, 0.00F, 0.00F, 0.40F),
                confidence = 1F,
                label = 0
            ),
            DetectionBox(
                rect = RectF(0.01F, 0.04F, 0.01F, 0.44F),
                confidence = 1F,
                label = 1
            ),
            DetectionBox(
                rect = RectF(0.02F, 0.08F, 0.02F, 0.48F),
                confidence = 1F,
                label = 2
            ),
            DetectionBox(
                rect = RectF(0.03F, 0.12F, 0.03F, 0.52F),
                confidence = 1F,
                label = 3
            ),
            DetectionBox(
                rect = RectF(0.04F, 0.16F, 0.04F, 0.56F),
                confidence = 1F,
                label = 4
            ),
            DetectionBox(
                rect = RectF(0.05F, 0.20F, 0.05F, 0.60F),
                confidence = 1F,
                label = 5
            ),
            DetectionBox(
                rect = RectF(0.06F, 0.24F, 0.06F, 0.64F),
                confidence = 1F,
                label = 6
            ),
            DetectionBox(
                rect = RectF(0.07F, 0.28F, 0.07F, 0.68F),
                confidence = 1F,
                label = 7
            ),
            DetectionBox(
                rect = RectF(0.08F, 0.32F, 0.08F, 0.72F),
                confidence = 1F,
                label = 8
            ),
            DetectionBox(
                rect = RectF(0.09F, 0.36F, 0.09F, 0.76F),
                confidence = 1F,
                label = 9
            ),
            DetectionBox(
                rect = RectF(0.10F, 0.40F, 0.10F, 0.80F),
                confidence = 1F,
                label = 0
            ),
            DetectionBox(
                rect = RectF(0.11F, 0.44F, 0.11F, 0.84F),
                confidence = 1F,
                label = 1
            ),
            DetectionBox(
                rect = RectF(0.12F, 0.48F, 0.12F, 0.88F),
                confidence = 1F,
                label = 2
            ),
            DetectionBox(
                rect = RectF(0.13F, 0.52F, 0.13F, 0.92F),
                confidence = 1F,
                label = 3
            ),
            DetectionBox(
                rect = RectF(0.14F, 0.56F, 0.14F, 0.96F),
                confidence = 1F,
                label = 4
            ),
            DetectionBox(
                rect = RectF(0.15F, 0.60F, 0.15F, 1.00F),
                confidence = 1F,
                label = 5
            )
        )

        assertEquals(
            "0123456789012345",
            determineLayoutAndFilter(numbers, VERTICAL_THRESHOLD)
                .map { it.label }
                .joinToString("")
        )
    }

    @Test
    fun determineLayoutAndFilter_linearDiagonalBottomLeftTopRight() {
        val numbers = listOf(
            DetectionBox(
                rect = RectF(0.00F, 0.60F, 0.00F, 1.00F),
                confidence = 1F,
                label = 0
            ),
            DetectionBox(
                rect = RectF(0.01F, 0.56F, 0.01F, 0.96F),
                confidence = 1F,
                label = 1
            ),
            DetectionBox(
                rect = RectF(0.02F, 0.52F, 0.02F, 0.92F),
                confidence = 1F,
                label = 2
            ),
            DetectionBox(
                rect = RectF(0.03F, 0.48F, 0.03F, 0.88F),
                confidence = 1F,
                label = 3
            ),
            DetectionBox(
                rect = RectF(0.04F, 0.44F, 0.04F, 0.84F),
                confidence = 1F,
                label = 4
            ),
            DetectionBox(
                rect = RectF(0.05F, 0.40F, 0.05F, 0.80F),
                confidence = 1F,
                label = 5
            ),
            DetectionBox(
                rect = RectF(0.06F, 0.36F, 0.06F, 0.76F),
                confidence = 1F,
                label = 6
            ),
            DetectionBox(
                rect = RectF(0.07F, 0.32F, 0.07F, 0.72F),
                confidence = 1F,
                label = 7
            ),
            DetectionBox(
                rect = RectF(0.08F, 0.28F, 0.08F, 0.68F),
                confidence = 1F,
                label = 8
            ),
            DetectionBox(
                rect = RectF(0.09F, 0.24F, 0.09F, 0.64F),
                confidence = 1F,
                label = 9
            ),
            DetectionBox(
                rect = RectF(0.10F, 0.20F, 0.10F, 0.60F),
                confidence = 1F,
                label = 0
            ),
            DetectionBox(
                rect = RectF(0.11F, 0.16F, 0.11F, 0.56F),
                confidence = 1F,
                label = 1
            ),
            DetectionBox(
                rect = RectF(0.12F, 0.12F, 0.12F, 0.52F),
                confidence = 1F,
                label = 2
            ),
            DetectionBox(
                rect = RectF(0.13F, 0.08F, 0.13F, 0.48F),
                confidence = 1F,
                label = 3
            ),
            DetectionBox(
                rect = RectF(0.14F, 0.04F, 0.14F, 0.44F),
                confidence = 1F,
                label = 4
            ),
            DetectionBox(
                rect = RectF(0.15F, 0.00F, 0.15F, 0.40F),
                confidence = 1F,
                label = 5
            )
        )

        assertEquals(
            "0123456789012345",
            determineLayoutAndFilter(numbers, VERTICAL_THRESHOLD)
                .map { it.label }
                .joinToString("")
        )
    }
}
