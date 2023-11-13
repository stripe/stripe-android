package com.stripe.android.payments.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent

/**
 * Name for injected set if strings to represent product usage for analytics.
 */
const val PRODUCT_USAGE = "productUsage"

/**
 * Name to indicate whether the current [StripeIntent] is a [PaymentIntent] or [SetupIntent].
 */
const val IS_PAYMENT_INTENT = "isPaymentIntent"

/**
 * Name to indicate whether the current app is an instant app.
 */
const val IS_INSTANT_APP = "isInstantApp"

/**
 * Status bar color of the host activity.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val STATUS_BAR_COLOR = "STATUS_BAR_COLOR"

internal const val INCLUDE_PAYMENT_SHEET_AUTHENTICATORS = "INCLUDE_PAYMENT_SHEET_AUTHENTICATORS"
