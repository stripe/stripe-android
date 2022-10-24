package com.stripe.android.core.injection

import androidx.annotation.RestrictTo

/**
 * Mark a class that can be injected by a [Injector].
 * This should be implemented by classes that cannot directly have their dependencies injected
 * through constructor and need to have them injected through lateinit properties.
 *
 * @param <FallbackInitializeParam> Class that holds the parameters required to recreate the Dagger
 * dependency graph during fallback.
 * @param <InjectorType> Injector class created during fallback, and used to inject into other
 * classes. [Unit] if the injector created during fallback does not inject into other classes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Injectable<FallbackInitializeParam, InjectorType> {
    /**
     * Fallback initialization logic for the dependencies when [Injector] is not available. This
     * could happen when the app process is killed and static [Injector]s are cleared up.
     *
     * An [Injectable] should check when this happens and calls this function manually.
     */
    fun fallbackInitialize(arg: FallbackInitializeParam): InjectorType
}
