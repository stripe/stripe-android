package com.stripe.android.connect

/**
 * Marks an API for Preview usage, meaning it can be changed or removed at any time (use at your own risk).
 * Some features in the Stripe Connect SDK are in preview and may be changed in the future without notice.
 */
@RequiresOptIn(message = "Some features in the Stripe Connect SDK are in preview. They may be changed in the future without notice.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class PreviewConnectSDK
