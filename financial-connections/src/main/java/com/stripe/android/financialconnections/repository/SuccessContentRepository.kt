package com.stripe.android.financialconnections.repository

import com.airbnb.mvrx.ExperimentalMavericksApi
import com.airbnb.mvrx.MavericksRepository
import com.airbnb.mvrx.MavericksState
import com.stripe.android.financialconnections.BuildConfig
import com.stripe.android.financialconnections.repository.SuccessContentRepository.State
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

internal interface SuccessContentRepository {
    suspend fun get(): State
    fun update(reducer: State.() -> State)

    data class State(
        val customSuccessMessage: TextResource? = null
    ) : MavericksState
}

@OptIn(ExperimentalMavericksApi::class)
internal class SuccessContentRepositoryImpl @Inject constructor(
    coroutineScope: CoroutineScope
) : SuccessContentRepository, MavericksRepository<State>(
    initialState = State(),
    coroutineScope = coroutineScope,
    performCorrectnessValidations = BuildConfig.DEBUG,
) {

    override suspend fun get() = awaitState()

    override fun update(reducer: State.() -> State) {
        setState(reducer)
    }
}
