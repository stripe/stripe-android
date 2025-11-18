package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize

internal abstract class LayoutCoordinateInitialVisibilityTracker(
    var expectedItems: List<String>,
    val visibilityThreshold: Int,
){

    protected data class CoordinateSnapshot(
        val positionInWindow: Offset,
        val size: IntSize,
        val boundsInWindow: Rect
    )

    var hasDispatched = false
        protected set

    fun updateVisibility(itemCode: String, coordinates: LayoutCoordinates) {
        if (itemCode !in expectedItems || expectedItems.isEmpty()) return
        if (hasDispatched) return // Only dispatch once per tracker instance
        /**
         * Capture only the relevant fields from [LayoutCoordinates] when we know the coordinates are attached.
         * Whether previous coordinates is/isn't attached or not is irrelevant, we only need to know
         * the previous coordinate's boundsInWindow, positionInWindow, and size.
         *
         * When implemented, [LayoutCoordinates.isAttached] provides a getter to a var, so we cannot rely on being able
         * to call `positionInWindow` and `boundsInWindow` of saved [LayoutCoordinates].
         */
        if (coordinates.isAttached) {
            val coordinateSnapshot = CoordinateSnapshot(
                positionInWindow = coordinates.positionInWindow(),
                size = coordinates.size,
                boundsInWindow = coordinates.boundsInWindow(),
            )

            val isVisible = calculateVisibility(
                coordinates = coordinateSnapshot,
                visibilityThresholdPercentage = visibilityThreshold,
            )

            updateVisibilityHelper(itemCode, coordinateSnapshot, isVisible)
        } else {
            return
        }
    }

    fun updateExpectedItems(items: List<String>) {
        if (this.expectedItems != items) {
            // Reset to initial state with new items
            this.expectedItems = items
            reset()
        }
    }

    abstract fun reset()

    protected abstract fun updateVisibilityHelper(
        itemCode: String,
        coordinateSnapshot: CoordinateSnapshot,
        isVisible: Boolean,
    )

    private fun calculateVisibility(
        coordinates: CoordinateSnapshot,
        visibilityThresholdPercentage: Int,
    ): Boolean {
        val bounds = coordinates.boundsInWindow

        // Check if completely out of bounds (hidden)
        @Suppress("ComplexCondition")
        if (bounds.left == 0f && bounds.top == 0f && bounds.right == 0f && bounds.bottom == 0f) {
            return false
        }

        // Calculate visibility percentage
        val widthInBounds = bounds.width
        val heightInBounds = bounds.height
        val totalArea = coordinates.size.height * coordinates.size.width
        val areaInBounds = widthInBounds * heightInBounds

        // 100 refers to percentages
        val percentVisible = if (totalArea > 0) {
            ((areaInBounds / totalArea) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        return percentVisible >= visibilityThresholdPercentage
    }
}
