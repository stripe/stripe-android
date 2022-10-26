package com.stripe.android.core.injection

import androidx.annotation.RestrictTo

/**
 * Mark a class that can be injected by a [Injector].
 * This should be implemented by classes that cannot directly have their dependencies injected
 * through constructor and need to have them injected through lateinit properties.
 *
 * @param <FallbackInitializeParam> Class that holds the parameters required to recreate the Dagger
 * dependency graph during fallback.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Injectable<FallbackInitializeParam> {
    /**
     * Fallback initialization logic for the dependencies when [Injector] is not available. This
     * could happen when the app process is killed and static [Injector]s are cleared up.
     * An [Injectable] should check when this happens and calls this function manually.
     *
     * @return The [Injector] created during fallback. It must be non-null when the [Injectable]
     * is responsible for injecting into other classes.
     */
    fun fallbackInitialize(arg: FallbackInitializeParam): Injector?
}
