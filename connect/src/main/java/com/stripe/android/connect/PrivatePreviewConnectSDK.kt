package com.stripe.android.connect

import androidx.annotation.RestrictTo

/**
 * Marks an API for Private Preview usage, meaning it can be changed or removed at any time (use at your own risk).
 * Some features in the Stripe Connect SDK are in private preview and may be changed in the future without notice.
 */
@RequiresOptIn(
    message = "Some features in the Stripe Connect SDK are in private preview" +
        " and may be changed in the future without notice."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
annotation class PrivatePreviewConnectSDK
