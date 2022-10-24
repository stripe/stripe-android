package com.stripe.android.core.injection

import androidx.annotation.RestrictTo

/**
 * Mark a class that can be injected by a [Injector] and does not support fallback.
 * It's implemented by ViewModel Factories when the ViewModel lifecycle is shorter than the activity
 * that contains it. In those cases, they receive the [Injector] directly as a constructor parameter
 * and should not be responsible for recreating the dependency graph.
 *
 * @see [Injectable]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface NonFallbackInjectable : Injectable<Unit, Unit> {

    /**
     * NonFallbackInjectable classes don't implement fallback because they receive the injector
     * directly as a constructor parameter.
     */
    override fun fallbackInitialize(arg: Unit) =
        throw IllegalStateException(
            "${this.javaClass.canonicalName} does not support injection fallback"
        )
}
