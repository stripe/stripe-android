package com.stripe.android.core.injection

import androidx.annotation.RestrictTo
import javax.inject.Qualifier

/**
 * Annotation to identify an [Injector] instance.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Qualifier
annotation class InjectorKey

/**
 * Mark a class that can inject into [Injectable]s.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Injector {
    /**
     * Injects into a [Injectable] instance.
     */
    fun inject(injectable: Injectable<*>)
}

/**
 * Dummy key when an [Injector] is not available.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val DUMMY_INJECTOR_KEY = "DUMMY_INJECTOR_KEY"
