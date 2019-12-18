package com.stripe.android.view

import com.stripe.android.StripeIntentResult
import com.stripe.android.model.StripeIntent

internal object StripeIntentResultExtras {
    /**
     * Should be a [StripeIntent.clientSecret]
     */
    const val CLIENT_SECRET = "client_secret"

    const val AUTH_EXCEPTION = "exception"

    /**
     * See [StripeIntentResult.Outcome] for possible values
     */
    const val FLOW_OUTCOME = "flow_outcome"

    const val SHOULD_CANCEL_SOURCE = "should_cancel_source"

    const val SOURCE_ID = "source_id"

    const val SOURCE = "source"
}
