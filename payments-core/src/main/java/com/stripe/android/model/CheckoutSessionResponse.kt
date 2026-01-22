package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * Response from checkout session APIs:
 * - Init API (`/v1/payment_pages/{cs_id}/init`)
 * - Confirm API (`/v1/payment_pages/{cs_id}/confirm`)
 *
 * Contains checkout session metadata and an embedded [ElementsSession] for PaymentSheet.
 * The [paymentIntent] field is only populated in confirm responses.
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

    /**
     * The PaymentIntent created/confirmed during checkout session confirmation.
     * Only populated in responses from the confirm API.
     */
    val paymentIntent: PaymentIntent? = null,
) : StripeModel
