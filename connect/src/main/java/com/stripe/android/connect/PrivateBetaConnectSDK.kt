package com.stripe.android.connect

import androidx.annotation.RestrictTo

/**
 * Marks an API for Private Beta usage, meaning it can be changed or removed at any time (use at your own risk).
 * The Stripe Connect SDK is in private beta and may be changed in the future without notice.
 */
@RequiresOptIn(message = "The Stripe Connect SDK is in private beta. It may be changed in the future without notice.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
annotation class PrivateBetaConnectSDK
