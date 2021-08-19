package com.stripe.android.payments.core.injection

import androidx.annotation.RestrictTo

/**
 * Mark a class that can be injected by a [Injector].
 * This should be implemented by classes that cannot directly have their dependencies injected
 * through constructor and need to have them injected through lateinit properties.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
interface Injectable
