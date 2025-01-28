package com.stripe.android.link

import com.stripe.android.core.exception.StripeException

internal class LinkEventException(override val cause: Throwable) : StripeException(cause = cause)
