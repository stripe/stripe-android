package com.stripe.android.payments.core.injection

import androidx.annotation.RestrictTo

/**
 * Mark a class that can be injected by a [Injector].
 * This should be implemented by classes that cannot directly have their dependencies injected
 * through constructor and need to have them injected through lateinit properties.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
interface Injectable<FallbackInitializeParam> {
    /**
     * Fallback initialization logic for the dependencies when [Injector] is not available. This
     * could happen when the app process is killed and static [Injector]s are cleared up.
     *
     * An [Injectable] should check when this happens and calls this function manually.
     */
    fun fallbackInitialize(arg: FallbackInitializeParam)
}
