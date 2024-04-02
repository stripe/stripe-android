package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal abstract class PersistingRepository<S : Parcelable>(
    private val savedStateHandle: SavedStateHandle,
) {

    private val key = makeKey()

    suspend fun await(
        timeout: Duration = 5.seconds,
    ): S {
        return withTimeout(timeout) {
            savedStateHandle.getStateFlow<S?>(key, null).filterNotNull().first()
        }
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
