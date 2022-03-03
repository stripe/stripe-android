package com.stripe.android.core.injection

import androidx.annotation.RestrictTo

/**
 * Name for injected boolean to denote if logging is enabled.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val ENABLE_LOGGING = "enableLogging"

/**
 * Name for user's publishable key
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val PUBLISHABLE_KEY = "publishableKey"

/**
 * Name for user's account id
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val STRIPE_ACCOUNT_ID = "stripeAccountId"
