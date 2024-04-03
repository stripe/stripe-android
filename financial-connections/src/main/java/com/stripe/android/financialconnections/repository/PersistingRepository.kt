package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle

internal abstract class PersistingRepository<S : Parcelable>(
    private val savedStateHandle: SavedStateHandle,
) {

    private val key = makeKey()

    fun get(): S? {
        return savedStateHandle[key]
    }

    fun set(state: S) {
        savedStateHandle[key] = state
    }

    fun clear() {
        savedStateHandle.remove<S>(key)
    }

    private fun makeKey(): String {
        return "PersistedState_${this::class.java.name}"
    }
}
