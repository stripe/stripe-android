package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.paymentelement.confirmation.ConfirmationMetadata

/**
 * The Stripe API request ID of the confirm call that preceded a next action or a local failure.
 */
internal object ConfirmRequestIdKey : ConfirmationMetadata.Key<String>
