package com.stripe.android.ui.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.Injector

/**
 * Mark an [Injector] that can inject into [NonFallbackInjectable] classes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface NonFallbackInjector : Injector
