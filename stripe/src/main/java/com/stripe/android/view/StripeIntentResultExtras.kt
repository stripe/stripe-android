package com.stripe.android.view

import com.stripe.android.StripeIntentResult
import com.stripe.android.model.PaymentIntent

internal object StripeIntentResultExtras {
    /**
     * Should be a [PaymentIntent.getClientSecret]
     */
    const val CLIENT_SECRET = "client_secret"

    const val AUTH_EXCEPTION = "exception"

    /**
     * See [StripeIntentResult.Outcome] for possible values
     */
    const val FLOW_OUTCOME = "flow_outcome"
}
