package com.stripe.android.financialconnections.repository

import com.airbnb.mvrx.MavericksState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class FinancialConnectionsErrorRepository {

    private val state = MutableStateFlow(State())

    suspend fun get() = state.value.error

    fun update(reducer: State.() -> State) {
        state.update { reducer(it) }
    }

    fun set(error: Throwable) {
        update {
            copy(error = error)
        }
    }

    fun clear() {
        update {
            copy(error = null)
        }
    }

    data class State(
        val error: Throwable? = null
    ) : MavericksState
}
