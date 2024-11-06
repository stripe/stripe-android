package com.stripe.android.view

/**
 * Configure [AddPaymentMethodActivity]'s UI and validation logic for billing address fields
 */
enum class BillingAddressFields {
    /**
     * Do not require any customer billing details when adding a new Payment Method
     */
    None,

    /**
     * Require the customer's postal code when adding a new Payment Method
     */
    PostalCode,

    /**
     * Require the customer's full billing address when adding a new Payment Method
     */
    Full
}
