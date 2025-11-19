package com.stripe.android.paymentsheet.verticalmode

internal class PaymentMethodLayoutInitialVisibilityTracker(
    private val callback: () -> Unit,
): LayoutCoordinateInitialVisibilityTracker(
    expectedItems = listOf(EMBEDDED_PAYMENT_METHOD_LAYOUT_NAME),
    visibilityThreshold = VISIBILITY_THRESHOLD_PERCENT_FOR_EMBEDDED_LAYOUT_TRACKING
) {

    override fun updateVisibilityHelper(
        itemCode: String,
        coordinateSnapshot: CoordinateSnapshot,
        isVisible: Boolean,
    ) {
        if(isVisible) {
            hasDispatched = true
            callback()
        }
    }

    override fun reset() {
        hasDispatched = false
    }

    companion object {
        const val EMBEDDED_PAYMENT_METHOD_LAYOUT_NAME = "embedded_payment_method_layout"

        /**
         * Arbitrary small non zero value to ensure that users see some of
         * embedded payment method layout before dispatching event.
         *
         * It is assumed that users will scroll and see the entire
         * embedded payment method layout before completing their purchase
         */
        const val VISIBILITY_THRESHOLD_PERCENT_FOR_EMBEDDED_LAYOUT_TRACKING = 25
    }
}
