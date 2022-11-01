package com.stripe.android.core.injection

import androidx.annotation.RestrictTo

/**
 * Mark an [Injector] that can inject into [NonFallbackInjectable] classes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface NonFallbackInjector : Injector
