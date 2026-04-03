package com.stripe.android.core.injection

import androidx.annotation.RestrictTo
import dagger.Component

/**
 * A registry to maintain [Injector] instances so that they can be retrieved from
 * [Activity]s and [Fragment]s.
 * This registry is needed for dagger injection because the SDK can't access [Application], and
 * thus [Activity]s can't get required [Component] from by downcasting  [Activity.getApplication].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface InjectorRegistry {
    /**
     * Registers an [Injector] instance with corresponding [InjectorKey].
     */
    fun register(injector: Injector, @InjectorKey key: String)

    /**
     * Retrieves an [Injector] instance from [InjectorKey].
     */
    fun retrieve(@InjectorKey injectorKey: String): Injector?

    /**
     * Returns the next key to identify an [Injector].
     */
    @InjectorKey
    fun nextKey(prefix: String): String
}
