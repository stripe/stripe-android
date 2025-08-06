package com.stripe.android.elements.payment

/**
 * Defines the layout orientations available for displaying payment methods in PaymentSheet.
 */
enum class PaymentMethodLayout {
    /**
     * Payment methods are arranged horizontally.
     * Users can swipe left or right to navigate through different payment methods.
     */
    Horizontal,

    /**
     * Payment methods are arranged vertically.
     * Users can scroll up or down to navigate through different payment methods.
     */
    Vertical,

    /**
     * This lets Stripe choose the best layout for payment methods in the sheet.
     */
    Automatic
}
