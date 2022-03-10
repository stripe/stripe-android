package com.stripe.android.link.injection

import com.stripe.android.core.injection.Injector

/**
 * Mark an [Injector] that can inject into [NonFallbackInjectable] classes.
 */
internal interface NonFallbackInjector : Injector
