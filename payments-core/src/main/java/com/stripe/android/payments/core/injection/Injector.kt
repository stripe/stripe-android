package com.stripe.android.payments.core.injection

import dagger.Component
import javax.inject.Qualifier

/**
 * Annotation to identify an [Injector] instance.
 */
@Qualifier
annotation class InjectorKey

/**
 * Mark a class that can inject into [Injectable]s. This interface is usually implemented by
 * dagger [Component] classes.
 */
internal abstract class Injector {
    /**
     * Injects into a [Injectable] instance.
     */
    abstract fun inject(injectable: Injectable)

    /**
     * A key to uniquely identify this [Injector] instance.
     */
    @InjectorKey
    var injectorKey: Int? = null
}
