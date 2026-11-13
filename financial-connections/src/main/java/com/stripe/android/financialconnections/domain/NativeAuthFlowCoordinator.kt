package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.di.ActivityRetainedScope
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

/**
 * The AuthFlow state is centralized in the parent viewModel.
 *
 * This component acts as a communication channel used by steps to send messages to parent
 * [com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel]
 *
 */
@ActivityRetainedScope
internal class NativeAuthFlowCoordinator @Inject constructor() {
    private val flow = MutableSharedFlow<Message>()

    operator fun invoke(): MutableSharedFlow<Message> {
        return flow
    }

    internal sealed interface Message {
        /**
         * Ensures partner web auth status gets cleared after the current session is finished.
         */
        data object ClearPartnerWebAuth : Message

        /**
         * Triggers a termination of the AuthFlow, completing the session in the current state.
         */
        data class Complete(
            val cause: EarlyTerminationCause? = null
        ) : Message {
            enum class EarlyTerminationCause(
                val value: String,
                val analyticsValue: String,
            ) {
                USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY(
                    value = "user_initiated_with_custom_manual_entry",
                    analyticsValue = "custom_manual_entry"
                )
            }
        }

        /**
         * Triggers a termination of the AuthFlow with an exception.
         */
        data class CloseWithError(
            val cause: Throwable
        ) : Message

        data class UpdateTopAppBar(
            val update: TopAppBarStateUpdate,
        ) : Message
    }
}
