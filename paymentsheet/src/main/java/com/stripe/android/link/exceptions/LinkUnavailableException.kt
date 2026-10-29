package com.stripe.android.link.exceptions

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException

/**
 * Link isn't available for the merchant, e.g. because it's turned off in their payment method settings.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkUnavailableException : StripeException(
    message = "Link is not available for this merchant. Check that Link is enabled in your payment method settings.",
) {
    override fun analyticsValue(): String = "linkUnavailable"
}
