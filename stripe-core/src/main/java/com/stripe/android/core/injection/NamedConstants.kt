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

/**
 * Name for form initial values
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val INITIAL_VALUES = "initialValues"

/**
 * Name for user's shipping address
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val SHIPPING_VALUES = "shippingValues"

/**
 * Name for isLiveMode
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val IS_LIVE_MODE = "isLiveMode"
