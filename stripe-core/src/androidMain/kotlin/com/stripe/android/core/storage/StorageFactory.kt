package com.stripe.android.core.storage

import android.content.Context
import androidx.annotation.RestrictTo

/**
 * Android entry point for shared [Storage] backed by SharedPreferences.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object StorageFactory {
    fun getStorageInstance(context: Context, purpose: String): Storage =
        SharedPreferencesStorage(context.applicationContext, purpose)
}
