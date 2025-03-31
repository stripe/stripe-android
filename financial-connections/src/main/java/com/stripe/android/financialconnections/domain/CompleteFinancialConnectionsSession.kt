package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import javax.inject.Inject

internal class CompleteFinancialConnectionsSession @Inject constructor(
    private val repository: FinancialConnectionsRepository,
    private val fetchPaginatedAccountsForSession: FetchPaginatedAccountsForSession,
    private val configuration: FinancialConnectionsSheetConfiguration
) {

    suspend operator fun invoke(
        earlyTerminationCause: NativeAuthFlowCoordinator.Message.Complete.EarlyTerminationCause?,
        closeAuthFlowError: Throwable?,
    ): Result {
        val session = repository.postCompleteFinancialConnectionsSessions(
            terminalError = earlyTerminationCause?.value,
            clientSecret = configuration.financialConnectionsSessionClientSecret,
        )

        val fullSession = fetchPaginatedAccountsForSession(session)

        return Result(
            session = fullSession,
            status = computeSessionCompletionStatus(fullSession, earlyTerminationCause, closeAuthFlowError),
        )
    }

    private fun computeSessionCompletionStatus(
        session: FinancialConnectionsSession,
        earlyTerminationCause: NativeAuthFlowCoordinator.Message.Complete.EarlyTerminationCause?,
        closeAuthFlowError: Throwable?,
    ): String {
        return earlyTerminationCause?.analyticsValue ?: session.completionStatus(closeAuthFlowError)
    }

    data class Result(
        val session: FinancialConnectionsSession,
        val status: String,
    )
}

private fun FinancialConnectionsSession.completionStatus(
    closeAuthFlowError: Throwable?,
): String {
    val completed = accounts.data.isNotEmpty() || paymentAccount != null || bankAccountToken != null
    return if (completed) {
        "completed"
    } else if (closeAuthFlowError != null) {
        "failed"
    } else {
        "canceled"
    }
}
