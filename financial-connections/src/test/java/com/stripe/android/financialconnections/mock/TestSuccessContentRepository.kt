package com.stripe.android.financialconnections.mock

import com.stripe.android.financialconnections.repository.SuccessContentRepository

internal class TestSuccessContentRepository() : SuccessContentRepository {

    var state = SuccessContentRepository.State()
    override suspend fun get(): SuccessContentRepository.State {
        return state
    }

    override fun update(reducer: SuccessContentRepository.State.() -> SuccessContentRepository.State) {
        state = state.reducer()
    }
}
