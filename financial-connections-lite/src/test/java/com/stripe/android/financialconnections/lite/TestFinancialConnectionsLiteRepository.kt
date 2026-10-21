package com.stripe.android.financialconnections.lite

import app.cash.turbine.Turbine
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.lite.repository.FinancialConnectionsLiteRepository
import com.stripe.android.financialconnections.lite.repository.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.model.FinancialConnectionsSession

internal class TestFinancialConnectionsLiteRepository(
    private val synchronizeResponse: Result<SynchronizeSessionResponse>,
    private val sessionResponse: Result<FinancialConnectionsSession>
) : FinancialConnectionsLiteRepository {
    val getSessionCalls = Turbine<Unit>()

    override suspend fun synchronize(
        configuration: FinancialConnectionsSheetConfiguration,
        applicationId: String,
    ): Result<SynchronizeSessionResponse> {
        return synchronizeResponse
    }

    override suspend fun getFinancialConnectionsSession(
        configuration: FinancialConnectionsSheetConfiguration,
    ): Result<FinancialConnectionsSession> {
        getSessionCalls.add(Unit)
        return sessionResponse
    }
}
