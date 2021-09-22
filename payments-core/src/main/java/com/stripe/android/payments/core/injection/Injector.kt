package com.stripe.android.payments.core.injection

import androidx.annotation.RestrictTo
import javax.inject.Qualifier

/**
 * Annotation to identify an [Injector] instance.
 */
@Qualifier
annotation class InjectorKey

/**
 * Mark a class that can inject into [Injectable]s.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
interface Injector {
    /**
     * Injects into a [Injectable] instance.
     */
    fun inject(injectable: Injectable<*>)
}

/**
 * Dummy key when an [Injector] is not available.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
const val DUMMY_INJECTOR_KEY = -1
