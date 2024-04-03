package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal abstract class PersistingRepository<S : Parcelable>(
    private val savedStateHandle: SavedStateHandle,
    private val initialValue: S,
) {

    private val key = makeKey()

    suspend fun await(
        timeout: Duration = 5.seconds,
    ): S {
        return savedStateHandle
            .getStateFlow<S?>(key, null)
            .filterNotNull()
            .timeout(timeout)
            .catch { emit(initialValue) }
            .first()
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
