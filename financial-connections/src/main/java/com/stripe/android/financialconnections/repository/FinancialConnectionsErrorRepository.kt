package com.stripe.android.financialconnections.repository

internal class FinancialConnectionsErrorRepository {

    private var state = State()

    fun get() = state.error

    fun update(reducer: State.() -> State) {
        state = reducer(state)
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
    )
}
