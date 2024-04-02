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

    suspend fun await(
        timeout: Duration = 5.seconds,
    ): S {
        return withTimeout(timeout) {
            savedStateHandle.getStateFlow<S?>(KeyState, null).filterNotNull().first()
        }
    }

    fun set(state: S) {
        savedStateHandle[KeyState] = state
    }

    fun clear() {
        savedStateHandle.remove<S>(KeyState)
    }

    companion object {
        const val KeyState = "State"
    }
}
