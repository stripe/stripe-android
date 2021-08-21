package com.stripe.android.payments.core.injection

import javax.inject.Qualifier

/**
 * Annotation to identify an [Injector] instance.
 */
@Qualifier
annotation class InjectorKey

/**
 * Mark a class that can inject into [Injectable]s.
 */
internal interface Injector {
    /**
     * Injects into a [Injectable] instance.
     */
    fun inject(injectable: Injectable)
}
