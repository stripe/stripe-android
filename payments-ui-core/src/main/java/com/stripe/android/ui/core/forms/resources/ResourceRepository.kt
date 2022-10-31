package com.stripe.android.ui.core.forms.resources

import androidx.annotation.RestrictTo

/**
 * Interface that provides resources needed by the forms.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ResourceRepository<T> {
    /**
     * Suspend function that will wait for resource to be loaded.
     * Must be called before trying to get any of the repositories.
     */
    suspend fun waitUntilLoaded()

    fun isLoaded(): Boolean

    fun getRepository(): T
}
