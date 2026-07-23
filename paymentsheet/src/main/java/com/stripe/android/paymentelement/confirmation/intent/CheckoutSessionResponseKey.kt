package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.paymentelement.confirmation.ConfirmationMetadata
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

/**
 * Carries the full [CheckoutSessionResponse] returned by the checkout session confirm call from
 * [CheckoutSessionConfirmationInterceptor] through the confirmation result, so the checkout state
 * can be refreshed with the post-confirm session.
 */
internal object CheckoutSessionResponseKey : ConfirmationMetadata.Key<CheckoutSessionResponse>
