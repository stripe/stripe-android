package com.stripe.android.financialconnections.exception

import com.stripe.android.financialconnections.model.FinancialConnectionsSession

/**
 * The AuthFlow was cancelled with no linked connected accounts.
 *
 * @param session the completed session. For more info about its cancellation,
 * see [FinancialConnectionsSession.statusDetails]
 *
 */
class AuthFlowCancelledError(
    val session: FinancialConnectionsSession
) : Exception()
