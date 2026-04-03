package com.stripe.android.core.model

/**
 * Common-source replacement for `@Parcelize`.
 *
 * Common code can annotate models with `@CommonParcelize` without importing any
 * Android type. On Android, the Parcelize compiler plugin is configured to
 * treat this annotation the same as `@Parcelize`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class CommonParcelize
