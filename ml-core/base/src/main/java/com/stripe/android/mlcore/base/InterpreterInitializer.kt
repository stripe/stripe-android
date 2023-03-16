package com.stripe.android.mlcore.base

import android.content.Context
import androidx.annotation.RestrictTo

/**
 * Initializer for TFLite Interpreter. Needs to be run before any inference calls.
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface InterpreterInitializer {
    /**
     * Initialize the encapsulated TFLite interpreter. Needs to be run on a non-UI thread.
     *
     * @param context: Application context
     * @param onSuccess: Notifies initialization success, can only run inference after this
     * @param onFailure: Notifies initialization failure
     */
    fun initialize(context: Context, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)
}
