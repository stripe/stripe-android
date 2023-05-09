package com.stripe.android.financialconnections.repository

import com.airbnb.mvrx.MavericksRepository
import com.airbnb.mvrx.MavericksState
import com.stripe.android.financialconnections.repository.PartnerToCoreAuthsRepository.State
import kotlinx.coroutines.CoroutineScope

internal class PartnerToCoreAuthsRepository(
    coroutineScope: CoroutineScope
) : MavericksRepository<State>(
    initialState = State(),
    coroutineScope = coroutineScope,
    performCorrectnessValidations = true,
) {

    suspend fun get() = awaitState().partnerToCoreAuths

    fun set(partnerToCoreAuths: Map<String, String>) {
        setState {
            copy(partnerToCoreAuths = partnerToCoreAuths)
        }
    }

    data class State(
        val partnerToCoreAuths: Map<String, String>? = null
    ) : MavericksState
}
