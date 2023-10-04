package com.stripe.android.financialconnections.repository

import com.airbnb.mvrx.MavericksRepository
import com.airbnb.mvrx.MavericksState
import com.stripe.android.financialconnections.BuildConfig
import com.stripe.android.financialconnections.repository.SaveToLinkWithStripeSucceededRepository.State
import kotlinx.coroutines.CoroutineScope

internal class SaveToLinkWithStripeSucceededRepository(
    coroutineScope: CoroutineScope
) : MavericksRepository<State>(
    initialState = State(),
    coroutineScope = coroutineScope,
    performCorrectnessValidations = BuildConfig.DEBUG,
) {

    suspend fun get() = awaitState().saveToLinkWithStripeSucceeded

    fun set(saveToLinkWithStripeSucceeded: Boolean) {
        setState {
            copy(saveToLinkWithStripeSucceeded = saveToLinkWithStripeSucceeded)
        }
    }

    data class State(
        val saveToLinkWithStripeSucceeded: Boolean? = null
    ) : MavericksState
}
