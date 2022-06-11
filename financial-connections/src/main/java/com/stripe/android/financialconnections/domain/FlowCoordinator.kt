package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.FlowCoordinatorMessage.RequestNextStep
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.navigation.NavigationCommand
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * After step completion, an updated [FinancialConnectionsSessionManifest] is received from
 * the API. This usecase can be triggered to update the manifest instance across the flow.
 *
 * Each step is responsible for consuming manifest updates as needed.
 */
@Singleton
internal class FlowCoordinator @Inject constructor() : ObserveFlowUpdates {
    val flow = MutableSharedFlow<FlowCoordinatorMessage>()

    override fun invoke(): SharedFlow<FlowCoordinatorMessage> {
        return flow
    }
}

internal sealed interface FlowCoordinatorMessage {

    /**
     * Updates global [FinancialConnectionsSessionManifest] instance.
     */
    data class UpdateManifest(
        val manifest: FinancialConnectionsSessionManifest
    ) : FlowCoordinatorMessage

    /**
     * Updates global [FinancialConnectionsAuthorizationSession] instance.
     */
    data class UpdateAuthorizationSession(
        val authorizationSession: FinancialConnectionsAuthorizationSession
    ) : FlowCoordinatorMessage

    /**
     * Request navigation to Next available Pane
     */
    data class RequestNextStep(
        val currentStep: NavigationCommand
    ) : FlowCoordinatorMessage

    object OpenWebAuthFlow : FlowCoordinatorMessage
}

/**
 * Only-read interface for [FlowCoordinator]
 */
internal interface ObserveFlowUpdates {
    operator fun invoke(): SharedFlow<FlowCoordinatorMessage>
}

internal class UpdateManifest @Inject constructor(
    private val logger: Logger,
    private val flowCoordinator: FlowCoordinator
) {
    suspend operator fun invoke(manifest: FinancialConnectionsSessionManifest) {
        logger.debug("Updating manifest")
        flowCoordinator.flow.emit(FlowCoordinatorMessage.UpdateManifest(manifest))
    }
}

internal class UpdateAuthorizationSession @Inject constructor(
    private val logger: Logger,
    private val flowCoordinator: FlowCoordinator
) {
    suspend operator fun invoke(manifest: FinancialConnectionsAuthorizationSession) {
        logger.debug("Updating Auth session")
        flowCoordinator.flow.emit(FlowCoordinatorMessage.UpdateAuthorizationSession(manifest))
    }
}

internal class RequestNextStep @Inject constructor(
    private val logger: Logger,
    private val flowCoordinator: FlowCoordinator
) {
    suspend operator fun invoke(currentStep: NavigationCommand) {
        logger.debug("Requesting next step")
        flowCoordinator.flow.emit(RequestNextStep(currentStep))
    }
}
