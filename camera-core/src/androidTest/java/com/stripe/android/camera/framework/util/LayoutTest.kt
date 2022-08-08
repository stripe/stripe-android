package com.stripe.android.camera.framework.util

import android.graphics.Rect
import android.util.Size
import androidx.test.filters.SmallTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LayoutTest {

    @Test
    @SmallTest
    fun maxAspectRatioInSize_sameRatio() {
        // the same aspect ratio as the size
        assertEquals(Size(16, 9), maxAspectRatioInSize(Size(16, 9), 16.toFloat() / 9))
    }

    @Test
    @SmallTest
    fun maxAspectRatioInSize_wide() {
        // an aspect ratio that's wider than tall
        assertEquals(Size(16, 9), maxAspectRatioInSize(Size(16, 16), 16.toFloat() / 9))
    }

    @Test
    @SmallTest
    fun maxAspectRatioInSize_tall() {
        // an aspect ratio that's taller than wide
        assertEquals(Size(9, 16), maxAspectRatioInSize(Size(16, 16), 9.toFloat() / 16))
    }

    @Test
    @SmallTest
    fun scaleAndCenterWithin_horizontal() {
        // center horizontally
        assertEquals(Rect(5, 0, 20, 15), Size(4, 4).scaleAndCenterWithin(Size(25, 15)))
    }

    @Test
    @SmallTest
    fun scaleAndCenterWithin_vertical() {
        // center vertically
        assertEquals(Rect(0, 5, 15, 20), Size(4, 4).scaleAndCenterWithin(Size(15, 25)))
    }

    @Test
    @SmallTest
    fun scaleAndCenterWithin_sameSquare() {
        // same ratio
        assertEquals(Rect(0, 0, 15, 15), Size(4, 4).scaleAndCenterWithin(Size(15, 15)))
    }

    @Test
    @SmallTest
    fun scaleAndCenterWithin_sameRectangle() {
        // same ratio, not square
        assertEquals(Rect(0, 0, 25, 15), Size(5, 3).scaleAndCenterWithin(Size(25, 15)))
    }

    @Test
    @SmallTest
    fun centerScaled_horizontal() {
        assertEquals(Rect(0, 0, 16, 8), Rect(4, 0, 12, 8).centerScaled(2F, 1F))
    }

    @Test
    @SmallTest
    fun centerScaled_vertical() {
        assertEquals(Rect(0, 0, 8, 16), Rect(0, 4, 8, 12).centerScaled(1F, 2F))
    }

    @Test
    @SmallTest
    fun centerScaled_sameSquare() {
        assertEquals(Rect(0, 0, 16, 16), Rect(4, 4, 12, 12).centerScaled(2F, 2F))
    }

    @Test
    @SmallTest
    fun centerScaled_sameRectangle() {
        assertEquals(Rect(0, 0, 8, 16), Rect(2, 4, 6, 12).centerScaled(2F, 2F))
    }

    @Test
    @SmallTest
    fun intersectionWith_sameRectangle() {
        assertEquals(Rect(0, 0, 15, 15), Rect(0, 0, 15, 15).intersectionWith(Rect(0, 0, 15, 15)))
    }

    @Test
    @SmallTest
    fun intersectionWith_parent() {
        assertEquals(Rect(2, 2, 15, 15), Rect(2, 2, 15, 15).intersectionWith(Rect(0, 0, 17, 17)))
    }

    @Test
    @SmallTest
    fun intersectionWith_child() {
        assertEquals(Rect(2, 2, 15, 15), Rect(0, 0, 17, 17).intersectionWith(Rect(2, 2, 15, 15)))
    }

    @Test
    @SmallTest
    fun intersectionWith_overlap() {
        assertEquals(Rect(2, 2, 15, 15), Rect(0, 0, 15, 15).intersectionWith(Rect(2, 2, 17, 17)))
    }

    @Test
    @SmallTest
    fun intersectionWith_noOverlap() {
        assertFailsWith<IllegalArgumentException>(
            "Given rects do not intersect",
            fun () {
                Rect(0, 0, 7, 7).intersectionWith(Rect(7, 7, 15, 15))
            }
        )
    }

    @Test
    @SmallTest
    fun move_vertical() {
        assertEquals(Rect(0, 0, 15, 15), Rect(0, 2, 15, 17).move(0, -2))
    }

    @Test
    @SmallTest
    fun move_horizontal() {
        assertEquals(Rect(0, 0, 15, 15), Rect(2, 0, 17, 15).move(-2, 0))
    }

    @Test
    @SmallTest
    fun move_both() {
        assertEquals(Rect(2, 2, 15, 15), Rect(4, 0, 17, 13).move(-2, 2))
    }

    @Test
    @SmallTest
    fun projectRegionOfInterest_smaller() {
        assertEquals(
            Rect(2, 14, 16, 28),
            Size(36, 84).projectRegionOfInterest(Size(18, 42), Rect(4, 28, 32, 56))
        )
    }

    @Test
    @SmallTest
    fun projectRegionOfInterest_exactFit() {
        assertEquals(
            Rect(0, 0, 18, 42),
            Size(36, 84).projectRegionOfInterest(Size(18, 42), Rect(0, 0, 36, 84))
        )
    }

    @Test
    @SmallTest
    fun projectRegionOfInterest_larger() {
        assertEquals(
            Rect(0, -1, 19, 42),
            Size(36, 84).projectRegionOfInterest(Size(18, 42), Rect(0, -2, 38, 84))
        )
    }

    @Test
    @SmallTest
    fun projectRegionOfInterest_noSize() {
        assertFailsWith<IllegalArgumentException>(
            "Cannot project from container with non-positive dimensions",
            fun () {
                Size(0, 0).projectRegionOfInterest(Size(18, 42), Rect(0, -2, 38, 84))
            }
        )
    }

    @Test
    @SmallTest
    fun projectRegionOfInterest_offCenter() {
        assertEquals(
            Rect(6, 14, 16, 20),
            Size(36, 84).projectRegionOfInterest(Size(18, 42), Rect(12, 28, 32, 40))
        )
    }

    @Test
    @SmallTest
    fun minAspectRatioSurroundingSize_squareVertical() {
        assertEquals(Size(900, 1800), minAspectRatioSurroundingSize(Size(900, 900), 0.5F))
    }

    @Test
    @SmallTest
    fun minAspectRatioSurroundingSize_squareHorizontal() {
        assertEquals(Size(1800, 900), minAspectRatioSurroundingSize(Size(900, 900), 2F))
    }

    @Test
    @SmallTest
    fun minAspectRatioSurroundingSize_rectangleVerticalToVertical() {
        assertEquals(Size(900, 1800), minAspectRatioSurroundingSize(Size(900, 1100), 0.5F))
    }

    @Test
    @SmallTest
    fun minAspectRatioSurroundingSize_rectangleVerticalToHorizontal() {
        assertEquals(Size(2200, 1100), minAspectRatioSurroundingSize(Size(900, 1100), 2F))
    }

    @Test
    @SmallTest
    fun minAspectRatioSurroundingSize_rectangleHorizontalToVertical() {
        assertEquals(Size(1100, 2200), minAspectRatioSurroundingSize(Size(1100, 900), 0.5F))
    }

    @Test
    @SmallTest
    fun minAspectRatioSurroundingSize_rectangleHorizontalToHorizontal() {
        assertEquals(Size(1800, 900), minAspectRatioSurroundingSize(Size(1100, 900), 2F))
    }

    @Test
    @SmallTest
    fun adjustSizeToAspectRatio_verticalCrop() {
        assertEquals(Size(900, 1800), adjustSizeToAspectRatio(Size(900, 2200), 0.5F))
    }

    @Test
    @SmallTest
    fun adjustSizeToAspectRatio_verticalExpand() {
        assertEquals(Size(900, 1800), adjustSizeToAspectRatio(Size(900, 1600), 0.5F))
    }

    @Test
    @SmallTest
    fun adjustSizeToAspectRatio_horizontalCrop() {
        assertEquals(Size(1800, 900), adjustSizeToAspectRatio(Size(2200, 900), 2F))
    }

    @Test
    @SmallTest
    fun adjustSizeToAspectRatio_horizontalExpand() {
        assertEquals(Size(1800, 900), adjustSizeToAspectRatio(Size(1600, 900), 2F))
    }
}
