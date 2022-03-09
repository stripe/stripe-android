package com.stripe.android.payments.core.injection

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
