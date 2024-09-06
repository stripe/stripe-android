package com.stripe.android.connectsdk

/**
 * Marks an API for Private Beta usage, meaning it can be changed or removed at any time (use at your own risk).
 * If you are interested in using a feature marked for private beta, send an email to
 * [private-beta-element@stripe.com](mailto:private-beta-element@stripe.com).
 */
@RequiresOptIn(message = "This API is under construction. If you're interested in using it, email private-beta-element@stripe.com.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class PrivateBetaConnectSDK
