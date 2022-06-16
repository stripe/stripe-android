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
internal class NativeAuthFlowCoordinator @Inject constructor() {
    private val flow = MutableSharedFlow<Message>()

    operator fun invoke(): MutableSharedFlow<Message> {
        return flow
    }

    internal sealed interface Message {

        /**
         * Updates global [FinancialConnectionsSessionManifest] instance.
         */
        data class UpdateManifest(
            val manifest: FinancialConnectionsSessionManifest
        ) : Message

        /**
         * Updates global [FinancialConnectionsAuthorizationSession] instance.
         */
        data class UpdateAuthorizationSession(
            val authorizationSession: FinancialConnectionsAuthorizationSession
        ) : Message

        /**
         * Request navigation to Next available Pane
         */
        data class RequestNextStep(
            val currentStep: NavigationCommand
        ) : Message

        object OpenWebAuthFlow : Message
    }
}
