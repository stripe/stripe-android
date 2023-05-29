package com.stripe.android.financialconnections.exception

import java.lang.Exception

/**
 * Base class for client errors that occur during the financial connections flow.
 */
internal class FinancialConnectionsClientError(
    val name: String,
    override val message: String
) : Exception()
