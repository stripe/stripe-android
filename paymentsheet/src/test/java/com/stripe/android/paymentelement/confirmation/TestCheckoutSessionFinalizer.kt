package com.stripe.android.paymentelement.confirmation

import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionConfirmationFinalizer
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionPoller

internal fun createTestCheckoutSessionFinalizer() = CheckoutSessionConfirmationFinalizer(
    poller = CheckoutSessionPoller { error("Unexpected Checkout Session poll") },
    retrieveSession = { error("Unexpected Checkout Session retrieval") },
    genericErrorMessage = "Something went wrong",
)
