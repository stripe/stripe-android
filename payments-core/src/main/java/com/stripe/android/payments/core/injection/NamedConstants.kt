package com.stripe.android.payments.core.injection

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent

/**
 * Name for injected set if strings to represent product usage for analytics.
 */
const val PRODUCT_USAGE = "productUsage"

/**
 * Name for user's publishable key
 */
const val PUBLISHABLE_KEY = "publishableKey"

/**
 * Name for user's account id
 */
const val STRIPE_ACCOUNT_ID = "stripeAccountId"

/**
 * Name to indicate whether the current [StripeIntent] is a [PaymentIntent] or [SetupIntent].
 */
const val IS_PAYMENT_INTENT = "isPaymentIntent"
