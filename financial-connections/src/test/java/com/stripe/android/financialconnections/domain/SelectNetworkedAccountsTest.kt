package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.model.Display
import com.stripe.android.financialconnections.model.ShareNetworkedAccountsResponse
import com.stripe.android.financialconnections.model.SuccessPane
import com.stripe.android.financialconnections.model.TextUpdate
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.coroutines.test.runTest
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

class SelectNetworkedAccountsTest {

    private val configuration = FinancialConnectionsSheetConfiguration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )
    private val successContentRepository: SuccessContentRepository =
        mock(SuccessContentRepository::class.java)
    private val repository: FinancialConnectionsAccountsRepository =
        mock(FinancialConnectionsAccountsRepository::class.java)
    private val selectNetworkedAccounts: SelectNetworkedAccounts =
        SelectNetworkedAccounts(configuration, successContentRepository, repository)

    @Test
    fun `invoke - if success message comes in the response, save it to repository`() = runTest {
        // Given
        val consumerSessionClientSecret = "test_client_secret"
        val selectedAccountIds = setOf("account_1", "account_2")
        val consentAcquired = true
        val successPane = SuccessPane("Success Caption", "Success SubCaption")
        val response = ShareNetworkedAccountsResponse(
            nextPane = null,
            display = Display(TextUpdate(successPane = successPane))
        )

        whenever(
            repository.postShareNetworkedAccounts(
                clientSecret = anyString(),
                consumerSessionClientSecret = anyString(),
                selectedAccountIds = anySet(),
                consentAcquired = anyOrNull()
            )
        ).thenReturn(response)

        // When
        selectNetworkedAccounts.invoke(
            consumerSessionClientSecret = consumerSessionClientSecret,
            selectedAccountIds = selectedAccountIds,
            consentAcquired = consentAcquired
        )

        // Then
        verify(successContentRepository).set(
            heading = TextResource.Text(successPane.caption),
            message = TextResource.Text(successPane.subCaption)
        )
    }
}
