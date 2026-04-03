package com.stripe.android.core.storage

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Storage {

    /**
     * Store a String in app storage by a [key].
     */
    fun storeValue(key: String, value: String): Boolean

    /**
     * Store a Long in app storage by a [key].
     */
    fun storeValue(key: String, value: Long): Boolean

    /**
     * Store an Int in app storage by a [key].
     */
    fun storeValue(key: String, value: Int): Boolean

    /**
     * Store a Float in app storage by a [key].
     */
    fun storeValue(key: String, value: Float): Boolean

    /**
     * Store a Boolean in app storage by a [key].
     */
    fun storeValue(key: String, value: Boolean): Boolean

    /**
     * Retrieve a String from app storage by a [key].
     */
    fun getString(key: String, defaultValue: String): String

    /**
     * Retrieve a Long from app storage by a [key].
     */
    fun getLong(key: String, defaultValue: Long): Long

    /**
     * Retrieve an Int from app storage by a [key].
     */
    fun getInt(key: String, defaultValue: Int): Int

    /**
     * Retrieve a Float from app storage by a [key].
     */
    fun getFloat(key: String, defaultValue: Float): Float

    /**
     * Retrieve a Boolean from app storage by a [key].
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    /**
     * Clears out a single value from storage.
     */
    fun remove(key: String): Boolean

    /**
     * Clear out all values from storage.
     */
    fun clear(): Boolean
}
