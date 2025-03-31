package com.stripe.android.financialconnections.lite

import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.lite.repository.FinancialConnectionsLiteRepository
import com.stripe.android.financialconnections.lite.repository.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.model.FinancialConnectionsSession

internal class TestFinancialConnectionsLiteRepository(
    private val synchronizeResponse: Result<SynchronizeSessionResponse>,
    private val sessionResponse: Result<FinancialConnectionsSession>
) : FinancialConnectionsLiteRepository {

    override suspend fun synchronize(
        configuration: FinancialConnectionsSheetConfiguration,
        applicationId: String,
    ): Result<SynchronizeSessionResponse> {
        return synchronizeResponse
    }

    override suspend fun getFinancialConnectionsSession(
        configuration: FinancialConnectionsSheetConfiguration,
    ): Result<FinancialConnectionsSession> {
        return sessionResponse
    }
}
