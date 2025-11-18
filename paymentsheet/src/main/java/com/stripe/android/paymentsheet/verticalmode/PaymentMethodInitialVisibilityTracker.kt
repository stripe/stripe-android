package com.stripe.android.paymentsheet.verticalmode

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
    expectedItems: List<String>,
    dispatcher: CoroutineContext = Dispatchers.Default,
    private val renderedLpmCallback: (List<String>, List<String>) -> Unit,
): LayoutCoordinateInitialVisibilityTracker(
    expectedItems = expectedItems,
    visibilityThreshold = DEFAULT_VISIBILITY_THRESHOLD_PERCENT
) {

    private val visibilityMap = mutableMapOf<String, Boolean>()
    private val previousCoordinateSnapshots = mutableMapOf<String, CoordinateSnapshot>()
    private val coordinateStabilityMap = mutableMapOf<String, Boolean>()

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
    override fun updateVisibilityHelper(
        itemCode: String,
        coordinateSnapshot: CoordinateSnapshot,
        isVisible: Boolean,
    ) {
        val previousCoordinatesForItem = previousCoordinateSnapshots[itemCode]

        // Check if coordinates are stable (haven't changed)
        // Compare position, size, and window bounds to detect any layout changes
        // All three must match exactly to consider coordinates stable
        val coordinatesAreStable = previousCoordinatesForItem?.let { prev ->
            prev.positionInWindow == coordinateSnapshot.positionInWindow &&
                prev.size == coordinateSnapshot.size &&
                prev.boundsInWindow == coordinateSnapshot.boundsInWindow
        } ?: false // First time seeing this item, so not stable yet

        // Update our tracking
        this.previousCoordinateSnapshots[itemCode] = coordinateSnapshot
        val wasVisibilityStable = visibilityMap[itemCode] == isVisible
        visibilityMap[itemCode] = isVisible

        if (coordinatesAreStable && wasVisibilityStable) {
            coordinateStabilityMap[itemCode] = true
        } else {
            coordinateStabilityMap.remove(itemCode)
            // Cancel any pending dispatch since coordinates changed
            dispatchEventJob?.cancel()
        }

        checkStabilityAndDispatch()
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

    override fun reset() {
        coordinateStabilityMap.clear()
        previousCoordinateSnapshots.clear()
        visibilityMap.clear()
        hasDispatched = false
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
         * 50ms allows bottom sheet animations and layout settling to complete
         * before capturing final visibility state.
         */
        private const val DEFAULT_DEBOUNCE_DELAY_MS = 50L
    }
}
