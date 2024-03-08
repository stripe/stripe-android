package com.stripe.android.financialconnections.repository

import com.airbnb.mvrx.MavericksRepository
import com.airbnb.mvrx.MavericksState
import com.stripe.android.financialconnections.BuildConfig
import com.stripe.android.financialconnections.repository.SuccessContentRepository.State
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.coroutines.CoroutineScope

internal class SuccessContentRepository(
    coroutineScope: CoroutineScope
) : MavericksRepository<State>(
    initialState = State(),
    coroutineScope = coroutineScope,
    performCorrectnessValidations = BuildConfig.DEBUG,
) {

    suspend fun get() = awaitState()

    fun update(reducer: State.() -> State) {
        setState(reducer)
    }

    data class State(
        val customSuccessMessage: TextResource? = null
    ) : MavericksState
}
