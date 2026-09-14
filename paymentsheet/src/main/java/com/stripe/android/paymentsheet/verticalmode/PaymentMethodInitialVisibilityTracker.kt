package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tracks stability of payment method positioning and dispatches analytics when stable
 *
 * This class monitors UI layout changes for payment methods and waits until:
 * 1. All expected payment methods have reported coordinates
 * 2. A debounce period passes without a layout change
 *
 * Once stable, it dispatches a single analytics event.
 */
internal class PaymentMethodInitialVisibilityTracker(
    private var expectedItems: List<String> = emptyList(),
    private val renderedLpmCallback: (List<String>, List<String>) -> Unit,
    private val coroutineScope: CoroutineScope,
) {
    private data class CoordinateSnapshot(
        val size: IntSize,
        val boundsInWindow: Rect
    )

    private val visibilityMap = mutableMapOf<String, Boolean>()
    private var hasDispatched = false

    private var dispatchEventJob: Job? = null

    fun updateExpectedItems(items: List<String>) {
        if (this.expectedItems != items) {
            // Reset to initial state with new items
            this.expectedItems = items
            reset()
        }
    }

    fun getHasDispatched(): Boolean {
        return this.hasDispatched
    }

    fun updateVisibility(itemCode: String, coordinates: LayoutCoordinates) {
        if (itemCode !in expectedItems || expectedItems.isEmpty()) return
        if (hasDispatched) return // Only dispatch once per tracker instance

        val coordinateSnapshot: CoordinateSnapshot
        if (coordinates.isAttached) {
            coordinateSnapshot = CoordinateSnapshot(
                size = coordinates.size,
                boundsInWindow = coordinates.boundsInWindow(),
            )
        } else {
            return
        }

        val newVisibility = calculateVisibility(coordinateSnapshot)
        visibilityMap[itemCode] = newVisibility
        dispatchEventJob?.cancel()

        checkStabilityAndDispatch()
    }

    @Suppress("MagicNumber")
    private fun calculateVisibility(coordinates: CoordinateSnapshot): Boolean {
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

        return percentVisible >= DEFAULT_VISIBILITY_THRESHOLD_PERCENT
    }

    private fun checkStability(): Boolean {
        return expectedItems.size == visibilityMap.size && expectedItems.all(visibilityMap::containsKey)
    }

    private fun checkStabilityAndDispatch() {
        // Prevent empty state dispatch
        val hasAnyVisible = visibilityMap.values.any { it }

        if (checkStability() && hasAnyVisible && !hasDispatched) {
            dispatchEventJob?.cancel()

            // Start a new job
            dispatchEventJob = coroutineScope.launch {
                // Wait for the debounce period to ensure animations are complete
                delay(DEFAULT_DEBOUNCE_DELAY_MS)
                if (!isActive) return@launch
                hasDispatched = true
                val visiblePaymentMethods = visibilityMap.filter { it.value }.keys.toList()
                val hiddenPaymentMethods = visibilityMap.filter { !it.value }.keys.toList()

                renderedLpmCallback(
                    visiblePaymentMethods,
                    hiddenPaymentMethods
                )
            }
        }
    }

    fun reset() {
        visibilityMap.clear()
        hasDispatched = false
        dispatchEventJob?.cancel()
        dispatchEventJob = null
    }

    companion object {
        /**
         * Minimum visibility percentage to consider a payment method "visible".
         * 95% threshold ensures we only count truly visible items, filtering out
         * tiny edge slivers during scrolling or animations.
         */
        private const val DEFAULT_VISIBILITY_THRESHOLD_PERCENT = 95

        /**
         * Debounce delay after UI stabilizes before dispatching analytics event.
         * 50ms allows bottom sheet animations and layout settling to complete
         * before capturing final visibility state.
         */
        private const val DEFAULT_DEBOUNCE_DELAY_MS = 50L
    }
}
