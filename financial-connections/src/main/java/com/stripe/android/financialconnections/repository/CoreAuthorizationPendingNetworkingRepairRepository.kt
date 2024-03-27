package com.stripe.android.financialconnections.repository

import com.airbnb.mvrx.MavericksState
import com.stripe.android.core.Logger

/**
 * Repository for storing the core authorization pending repair.
 *
 */
internal class CoreAuthorizationPendingNetworkingRepairRepository(
    private val logger: Logger
) {

    private var state: State = State()

    fun get() = state.coreAuthorization

    fun set(coreAuthorization: String) {
        logger.debug("core authorization set to $coreAuthorization")
        state = state.copy(
            coreAuthorization = coreAuthorization
        )
    }

    data class State(
        val coreAuthorization: String? = null
    ) : MavericksState
}
