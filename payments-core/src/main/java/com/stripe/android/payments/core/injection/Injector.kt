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
interface Injector
