package com.stripe.android.financialconnections.exception

import com.stripe.android.core.exception.StripeException

/**
 * The AuthFlow was prematurely cancelled due to user requesting manual entry.
 *
 */
class CustomManualEntryRequiredError : StripeException()
