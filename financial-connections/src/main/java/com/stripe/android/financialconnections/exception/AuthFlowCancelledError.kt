package com.stripe.android.financialconnections.exception

import com.stripe.android.financialconnections.model.FinancialConnectionsSession

/**
 * The AuthFlow was cancelled prematurely.
 *
 * @param session the completed session. For more info about its cancellation,
 * see [FinancialConnectionsSession.statusDetails]
 *
 */
class AuthFlowCancelledError(
    val session: FinancialConnectionsSession
) : Exception()
