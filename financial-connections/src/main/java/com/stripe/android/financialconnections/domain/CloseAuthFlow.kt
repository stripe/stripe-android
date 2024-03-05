package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.exception.CustomManualEntryRequiredError
import com.stripe.android.financialconnections.features.manualentry.isCustomManualEntryError
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import javax.inject.Inject

/**
 * There's at least three types of close cases:
 * 1. User closes (with or without an error),
 *    and fetching accounts returns accounts (or `paymentAccount`). That's a success.
 * 2. User closes with an error, and fetching accounts returns NO accounts. That's an error.
 * 3. User closes without an error, and fetching accounts returns NO accounts. That's a cancel.
 *
 * @return [FinancialConnectionsSheetActivityResult]
 */
internal class CloseAuthFlow @Inject constructor(
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger,
) {

    /**
     * @param sessionFetcher a suspend function that obtains the terminal [FinancialConnectionsSession]
     * - web: fetches the already completed session from backend
     * - native: uses the complete session endpoint, that returns a final session.
     * @param closeAuthFlowError an error that occurred while closing the auth flow
     */
    suspend operator fun invoke(
        sessionFetcher: suspend () -> FinancialConnectionsSession,
        closeAuthFlowError: Throwable?,
        fromNative: Boolean
    ): FinancialConnectionsSheetActivityResult = runCatching {
        val session = sessionFetcher()
        if (fromNative) eventTracker.track(
            FinancialConnectionsAnalyticsEvent.Complete(
                exception = null,
                exceptionExtraMessage = null,
                connectedAccounts = session.accounts.data.count()
            )
        )
        when {
            session.isCustomManualEntryError() -> {
                FinancialConnections.emitEvent(FinancialConnectionsEvent.Name.MANUAL_ENTRY_INITIATED)
                Failed(error = CustomManualEntryRequiredError())
            }

            session.hasAValidAccount() -> {
                FinancialConnections.emitEvent(
                    name = FinancialConnectionsEvent.Name.SUCCESS,
                    metadata = FinancialConnectionsEvent.Metadata(
                        manualEntry = session.paymentAccount is BankAccount,
                    )
                )
                FinancialConnectionsSheetActivityResult.Completed(
                    financialConnectionsSession = session,
                    token = session.parsedToken

                )
            }

            closeAuthFlowError != null ->
                Failed(error = closeAuthFlowError)


            else -> {
                FinancialConnections.emitEvent(FinancialConnectionsEvent.Name.CANCEL)
                FinancialConnectionsSheetActivityResult.Canceled
            }
        }
    }.getOrElse { completeSessionError ->
        val errorMessage = "Error completing session before closing"
        logger.error(errorMessage, completeSessionError)
        if (fromNative) eventTracker.track(
            FinancialConnectionsAnalyticsEvent.Complete(
                exception = completeSessionError,
                exceptionExtraMessage = errorMessage,
                connectedAccounts = null
            )
        )
        Failed(closeAuthFlowError ?: completeSessionError)
    }

    private fun FinancialConnectionsSession.hasAValidAccount() =
        accounts.data.isNotEmpty() ||
            paymentAccount != null ||
            bankAccountToken != null
}

