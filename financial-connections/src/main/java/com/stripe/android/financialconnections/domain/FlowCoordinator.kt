package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.navigation.NavigationCommand
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The AuthFlow state is centralized in the parent viewModel.
 *
 * This component acts as a communication channel used by steps to send messages to parent
 * [com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel]
 *
 */
@Singleton
internal class FlowCoordinator @Inject constructor() {
    private val flow = MutableSharedFlow<FlowCoordinatorMessage>()

    operator fun invoke(): MutableSharedFlow<FlowCoordinatorMessage> {
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
