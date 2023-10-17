package com.stripe.android.financialconnections.domain

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
         * Ensures partner web auth status gets cleared after the current session is finished.
         */
        object ClearPartnerWebAuth : Message

        /**
         * Triggers a termination of the AuthFlow, completing the session in the current state.
         */
        data class Complete(
            val cause: EarlyTerminationCause? = null
        ) : Message {
            enum class EarlyTerminationCause(val value: String) {
                USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY("user_initiated_with_custom_manual_entry")
            }
        }
    }
}
