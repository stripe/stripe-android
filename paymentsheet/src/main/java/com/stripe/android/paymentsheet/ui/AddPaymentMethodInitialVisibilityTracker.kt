package com.stripe.android.paymentsheet.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times

internal object AddPaymentMethodInitialVisibilityTracker {

    /**
     * Minimum visibility percentage to consider a payment method "visible".
     * 95% threshold ensures we only count truly visible items, filtering out
     * tiny edge slivers during scrolling or animations.
     */
    private const val DEFAULT_VISIBILITY_THRESHOLD_PERCENT = 0.95f

    internal fun reportInitialPaymentMethodVisibilitySnapshot(
        data: AddPaymentMethodInitialVisibilityTrackerData,
        callback: (List<String>, List<String>) -> Unit,
    ) {
        val numberOfVisibleItems = calculateNumberOfVisibleItems(
            totalItems = data.paymentMethodCodes.size,
            tabWidth = data.tabWidth,
            screenWidth = data.screenWidth,
            innerPadding = data.innerPadding
        )

        callback.invoke(
            data.paymentMethodCodes.take(numberOfVisibleItems), // visible payment methods
            data.paymentMethodCodes.drop(numberOfVisibleItems) // hidden payment methods
        )
    }

    /**
     * Calculates the number of payment method tabs that are visible on screen.
     */
    private fun calculateNumberOfVisibleItems(
        totalItems: Int,
        tabWidth: Dp,
        screenWidth: Dp,
        innerPadding: Dp,
    ): Int {
        if (totalItems <= 0) return 0
        if (totalItems == 1) return 1

        // Calculate how many items can fit with their spacing
        val itemWithPadding = tabWidth + innerPadding
        val maxItemsThatFit = (screenWidth / itemWithPadding).toInt()

        // Check if there's enough remaining space for a partially visible item
        val usedWidth = maxItemsThatFit * itemWithPadding
        val remainingWidth = screenWidth - usedWidth

        // Consider an item visible if at least 95% of it is shown
        @Suppress("MagicNumber")
        val visibilityThreshold = tabWidth * DEFAULT_VISIBILITY_THRESHOLD_PERCENT
        val hasPartiallyVisibleItem = remainingWidth >= visibilityThreshold

        val totalVisibleItems = if (hasPartiallyVisibleItem) {
            maxItemsThatFit + 1
        } else {
            maxItemsThatFit
        }

        // Ensure we don't exceed the total number of items and always show at least 1
        return totalVisibleItems.coerceIn(1, totalItems)
    }
}

internal data class AddPaymentMethodInitialVisibilityTrackerData (
    val paymentMethodCodes: List<String>,
    val tabWidth: Dp,
    val screenWidth: Dp,
    val innerPadding: Dp,
)
