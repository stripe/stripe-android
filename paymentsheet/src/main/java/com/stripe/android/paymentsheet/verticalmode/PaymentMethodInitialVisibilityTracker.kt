package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Tracks stability of payment method positioning and dispatches analytics when stable
 *
 * This class monitors UI layout changes for payment methods and waits until:
 * 1. All expected payment methods have stable coordinates (no layout changes)
 * 2. All visibility states are stable (no visibility changes)
 * 3. A debounce period passes to ensure animations are complete
 *
 * Once stable, it dispatches a single analytics event.
 */
internal class PaymentMethodInitialVisibilityTracker(
    private val expectedItems: List<String>,
    private val renderedLpmCallback: (List<String>, List<String>) -> Unit,
    dispatcher: CoroutineContext = Dispatchers.Default,
) {
    private val visibilityMap = mutableMapOf<String, Boolean>()
    private val previousCoordinates = mutableMapOf<String, LayoutCoordinates>()
    private val coordinateStabilityMap = mutableMapOf<String, Boolean>()
    private var hasDispatched = false

    private val coroutineScope = CoroutineScope(dispatcher)
    private var dispatchEventJob: Job? = null

    /**
     * When this function is called from onGloballyPositioned
     * it is guaranteed to be called twice on any given stable coordinate
     *
     * onGloballyPositioned is called
     * when coordinates are first available
     * when coordinates have moved
     * and when the composition is finalized and stable.
     */
    fun updateVisibility(itemCode: String, coordinates: LayoutCoordinates) {
        if (itemCode !in expectedItems || expectedItems.isEmpty()) return
        if (!coordinates.isAttached) return
        if (hasDispatched) return // Only dispatch once per tracker instance

        val newVisibility = calculateVisibility(coordinates)
        val previousCoordinatesForItem = previousCoordinates[itemCode]

        // Check if coordinates are stable (haven't changed)
        // Compare position, size, and window bounds to detect any layout changes
        // All three must match exactly to consider coordinates stable
        val coordinatesAreStable = previousCoordinatesForItem?.let { prev ->
            prev.positionInWindow() == coordinates.positionInWindow() &&
                prev.size == coordinates.size &&
                prev.boundsInWindow() == coordinates.boundsInWindow()
        } ?: false // First time seeing this item, so not stable yet

        // Update our tracking
        this.previousCoordinates[itemCode] = coordinates
        val wasVisibilityStable = visibilityMap[itemCode] == newVisibility
        visibilityMap[itemCode] = newVisibility

        if (coordinatesAreStable && wasVisibilityStable) {
            coordinateStabilityMap[itemCode] = true
        } else {
            coordinateStabilityMap.remove(itemCode)
            // Cancel any pending dispatch since coordinates changed
            dispatchEventJob?.cancel()
        }

        checkStabilityAndDispatch()
    }

    @Suppress("MagicNumber")
    private fun calculateVisibility(coordinates: LayoutCoordinates): Boolean {
        val bounds = coordinates.boundsInWindow()

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
        if (expectedItems.size != coordinateStabilityMap.size || expectedItems.size != visibilityMap.size) {
            return false
        }

        return expectedItems.all {
            coordinateStabilityMap[it] == true && visibilityMap.containsKey(it)
        }
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

    fun dispose() {
        dispatchEventJob?.cancel()
        coroutineScope.cancel() // Cancel the entire scope
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
         * 800ms allows bottom sheet animations and layout settling to complete
         * before capturing final visibility state.
         */
        private const val DEFAULT_DEBOUNCE_DELAY_MS = 800L
    }
}
