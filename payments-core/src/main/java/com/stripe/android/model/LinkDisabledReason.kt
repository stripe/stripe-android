package com.stripe.android.model

import androidx.annotation.RestrictTo

/**
 * Reasons why Link may be disabled.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class LinkDisabledReason(val value: String) {
    /**
     * The Elements session response indicates that Link isn't supported.
     */
    NotSupportedInElementsSession("not_supported_in_elements_session"),

    /**
     * Link is disabled via `PaymentSheet.LinkConfiguration`.
     */
    LinkConfiguration("link_configuration"),

    /**
     * Card brand filtering is requested and native Link isn't available.
     */
    CardBrandFiltering("card_brand_filtering"),

    /**
     * Billing details collection is requested and native Link isn't available.
     */
    BillingDetailsCollection("billing_details_collection")
}
