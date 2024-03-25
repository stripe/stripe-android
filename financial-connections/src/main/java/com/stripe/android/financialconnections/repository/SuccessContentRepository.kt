package com.stripe.android.financialconnections.repository

import com.stripe.android.financialconnections.repository.SuccessContentRepository.State
import com.stripe.android.financialconnections.ui.TextResource
import javax.inject.Inject

internal interface SuccessContentRepository {
    suspend fun get(): State
    fun update(reducer: State.() -> State)

    data class State(
        val customSuccessMessage: TextResource? = null
    )
}

internal class SuccessContentRepositoryImpl @Inject constructor() : SuccessContentRepository {

    private var state = State()

    override suspend fun get() = state

    override fun update(reducer: State.() -> State) {
        state = reducer(state)
    }
}
