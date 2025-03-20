package com.stripe.android.financialconnections.lite

import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.lite.repository.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.lite.repository.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession

internal object TextFixtures {

    val configuration = FinancialConnectionsSheetConfiguration(
        financialConnectionsSessionClientSecret = "client_secret_123",
        publishableKey = "pk_test_123",
    )

    val syncResponse = SynchronizeSessionResponse(
        manifest = FinancialConnectionsSessionManifest(
            hostedAuthUrl = "https://example.com/auth",
            successUrl = "https://example.com/success",
            cancelUrl = "https://example.com/cancel",
        )
    )

    val financialConnectionsSessionNoAccounts = FinancialConnectionsSession(
        clientSecret = "las_1234567890",
        id = "fcsess_secret",
        accountsNew = FinancialConnectionsAccountList(
            data = emptyList(),
            hasMore = false,
            url = "url",
            count = 0
        ),
        livemode = true
    )
}
