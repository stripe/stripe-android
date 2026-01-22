package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * Response from checkout session APIs:
 * - Init API (`/v1/payment_pages/{cs_id}/init`) - returns [elementsSession]
 * - Confirm API (`/v1/payment_pages/{cs_id}/confirm`) - returns [paymentIntent]
 *
 * For init responses, [elementsSession] contains payment method preferences, Link settings,
 * customer data, and other configuration needed by PaymentSheet.
 * For confirm responses, [paymentIntent] contains the confirmed payment intent.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class CheckoutSessionResponse(
    /**
     * The checkout session ID (e.g., "cs_xxx").
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
     * Only populated in responses from the init API.
     */
    val elementsSession: ElementsSession? = null,

    /**
     * The PaymentIntent created/confirmed during checkout session confirmation.
     * Only populated in responses from the confirm API.
     */
    val paymentIntent: PaymentIntent? = null,
) : StripeModel
