package com.stripe.android.financialconnections.repository

import com.stripe.android.financialconnections.repository.SuccessContentRepository.State
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

internal interface SuccessContentRepository {
    suspend fun get(): State
    fun update(reducer: State.() -> State)

    data class State(
        val customSuccessMessage: TextResource? = null
    )
}

internal class SuccessContentRepositoryImpl @Inject constructor() : SuccessContentRepository {

    private val state = MutableStateFlow(State())

    override suspend fun get() = state.value

    override fun update(reducer: State.() -> State) {
        state.update { reducer(it) }
    }
}
