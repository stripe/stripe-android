package com.stripe.android.financialconnections.repository

import com.airbnb.mvrx.MavericksRepository
import com.airbnb.mvrx.MavericksState
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.repository.CoreAuthorizationPendingNetworkingRepairRepository.State
import kotlinx.coroutines.CoroutineScope

/**
 * Repository for storing the core authorization pending repair.
 *
 */
internal class CoreAuthorizationPendingNetworkingRepairRepository(
    coroutineScope: CoroutineScope,
    private val logger: Logger,
    private val analyticsTracker: FinancialConnectionsAnalyticsTracker
) : MavericksRepository<State>(
    initialState = State(),
    coroutineScope = coroutineScope,
    performCorrectnessValidations = BuildConfig.DEBUG,
) {

    suspend fun get() = runCatching {
        awaitState().coreAuthorization
    }.onFailure {
        analyticsTracker.logError(
            "Failed to get core authorization",
            logger = logger,
            pane = Pane.UNEXPECTED_ERROR,
            error = it
        )
    }.getOrNull()

    suspend fun set(coreAuthorization: String) = runCatching {
        logger.debug("core authorization set to $coreAuthorization")
        setState { copy(coreAuthorization = coreAuthorization) }
    }.onFailure {
        analyticsTracker.logError(
            "Failed to set core authorization",
            logger = logger,
            pane = Pane.UNEXPECTED_ERROR,
            error = it
        )
    }

    data class State(
        val coreAuthorization: String? = null
    ) : MavericksState
}
