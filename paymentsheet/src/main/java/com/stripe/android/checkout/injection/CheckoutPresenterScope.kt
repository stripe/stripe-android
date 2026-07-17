package com.stripe.android.checkout.injection

import javax.inject.Scope

/**
 * Scopes the per-presenter graph created by [CheckoutPresenterSubcomponent] — the activity-bound
 * pieces (sheet launcher, initializer) that must share a single instance for one
 * [android.app.Activity] and be recreated when a new presenter is created.
 */
@Scope
@Retention(AnnotationRetention.RUNTIME)
internal annotation class CheckoutPresenterScope
