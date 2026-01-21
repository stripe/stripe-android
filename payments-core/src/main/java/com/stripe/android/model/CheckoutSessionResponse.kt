package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * Response from the checkout session init API (`/v1/payment_pages/{cs_id}/init`).
 *
 * Contains both checkout session metadata and an embedded [ElementsSession] for PaymentSheet.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class CheckoutSessionResponse(
    /**
     * The checkout session ID (e.g., "cs_test_xxx").
     */
    val id: String,

    /**
     * The payment amount in the smallest currency unit (e.g., cents for USD).
     */
    val amount: Long,

    /**
     * The three-letter ISO currency code (e.g., "usd").
     */
    val currency: String,

    /**
     * The embedded ElementsSession containing payment method preferences, Link settings,
     * customer data, and other configuration needed by PaymentSheet.
     */
    val elementsSession: ElementsSession,
) : StripeModel
