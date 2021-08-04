package com.stripe.android.payments.core.injection

import android.app.Activity
import android.app.Application
import androidx.fragment.app.Fragment
import dagger.Component

/**
 * A registry to maintain [Injector] instances so that they can be retrieved from
 * [Activity]s and [Fragment]s.
 * This registry is needed for dagger injection because the SDK can't access [Application], and
 * thus [Activity]s can't get required [Component] from by downcasting  [Activity.getApplication].
 */
interface InjectorRegistry {
    /**
     * Registers an [Injector] instance with corresponding [InjectorKey]
     */
    fun register(injector: Injector, @InjectorKey key: Int)

    /**
     * Retrieves an [Injector] instance from [InjectorKey].
     */
    fun retrieve(@InjectorKey injectorKey: Int): Injector?

    /**
     * Returns the next key to identify an [Injector]
     */
    @InjectorKey
    fun nextKey(): Int
}
