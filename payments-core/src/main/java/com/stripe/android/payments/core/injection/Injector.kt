package com.stripe.android.payments.core.injection

import javax.inject.Qualifier

/**
 * Annotation to identify an [Injector] instance.
 */
@Qualifier
annotation class InjectorKey

/**
 * Mark a class that can inject into [Injectable]s. A [Injector] is responsible for saving a
 * unique identifier [InjectorKey] assigned to it.
 */
internal interface Injector {
    /**
     * Injects into a [Injectable] instance.
     */
    fun inject(injectable: Injectable)

    /**
     * Returns the [InjectorKey] that uniquely identifies this [Injector].
     */
    @InjectorKey
    fun getInjectorKey(): Int?

    /**
     * Sets unique [InjectorKey] to this [Injector].
     */
    fun setInjectorKey(@InjectorKey injectorKey: Int)
}
