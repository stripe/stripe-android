package com.stripe.android.financialconnections.lite.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.lite.network.FinancialConnectionsLiteRequestExecutor
import org.junit.Test
import org.mockito.kotlin.mock

internal class FinancialConnectionsLiteRepositoryTest {

    @Test
    fun `permissioned sessions use supplied merchant credentials`() {
        val repository = FinancialConnectionsLiteRepositoryImpl(
            requestExecutor = mock<FinancialConnectionsLiteRequestExecutor>(),
            apiRequestFactory = mock<ApiRequest.Factory>(),
        )
        val configuration = FinancialConnectionsSheetConfiguration(
            financialConnectionsSessionClientSecret = "fcsess_secret",
            publishableKey = "pk_merchant",
            stripeAccountId = "acct_merchant",
            hasRequestedDataPermissions = true,
            existingConsumer = null,
        )

        val options = with(repository) { configuration.apiRequestOptions() }

        assertThat(options.apiKey).isEqualTo("pk_merchant")
        assertThat(options.stripeAccount).isEqualTo("acct_merchant")
    }
}
