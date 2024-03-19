package com.stripe.android.financialconnections.repository

import com.airbnb.mvrx.ExperimentalMavericksApi
import com.airbnb.mvrx.MavericksRepository
import com.airbnb.mvrx.MavericksState
import com.stripe.android.financialconnections.BuildConfig
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMavericksApi::class)
internal class FinancialConnectionsErrorRepository(
    coroutineScope: CoroutineScope
) : MavericksRepository<FinancialConnectionsErrorRepository.State>(
    initialState = State(),
    coroutineScope = coroutineScope,
    performCorrectnessValidations = BuildConfig.DEBUG,
) {

    suspend fun get() = awaitState()

    fun set(
        error: Throwable,
        retryPane: Pane?,
    ) {
        setState {
            copy(error = error, retryPane = retryPane)
        }
    }

    fun clear() {
        setState {
            copy(error = null, retryPane = null)
        }
    }

    data class State(
        val error: Throwable? = null,
        val retryPane: Pane? = null,
    ) : MavericksState
}
