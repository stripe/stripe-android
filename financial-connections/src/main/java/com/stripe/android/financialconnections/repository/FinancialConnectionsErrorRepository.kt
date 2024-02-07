package com.stripe.android.financialconnections.repository

import com.airbnb.mvrx.ExperimentalMavericksApi
import com.airbnb.mvrx.MavericksRepository
import com.airbnb.mvrx.MavericksState
import com.stripe.android.financialconnections.BuildConfig
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMavericksApi::class)
internal class FinancialConnectionsErrorRepository(
    coroutineScope: CoroutineScope
) : MavericksRepository<FinancialConnectionsErrorRepository.State>(
    initialState = State(),
    coroutineScope = coroutineScope,
    performCorrectnessValidations = BuildConfig.DEBUG,
) {

    suspend fun get() = awaitState().error

    fun set(error: Throwable) {
        setState {
            copy(error = error)
        }
    }

    fun clear() {
        setState {
            copy(error = null)
        }
    }

    data class State(
        val error: Throwable? = null
    ) : MavericksState
}
