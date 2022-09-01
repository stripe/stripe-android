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
        object OpenWebAuthFlow : Message
        object Finish : Message
    }
}
