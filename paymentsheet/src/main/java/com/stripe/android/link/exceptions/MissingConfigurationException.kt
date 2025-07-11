package com.stripe.android.link.exceptions

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MissingConfigurationException : StripeException()
