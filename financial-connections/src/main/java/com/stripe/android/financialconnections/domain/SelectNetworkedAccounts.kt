package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.ShareNetworkedAccountsResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.ui.TextResource
import javax.inject.Inject

internal class SelectNetworkedAccounts @Inject constructor(
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val successContentRepository: SuccessContentRepository,
    private val repository: FinancialConnectionsAccountsRepository
) {

    suspend operator fun invoke(
        consumerSessionClientSecret: String,
        selectedAccountIds: Set<String>,
        consentAcquired: Boolean?
    ): ShareNetworkedAccountsResponse {
        return repository.postShareNetworkedAccounts(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            consumerSessionClientSecret = consumerSessionClientSecret,
            selectedAccountIds = selectedAccountIds,
            consentAcquired = consentAcquired
        ).also { response ->
            response.display?.text?.successPane?.let { successPane ->
                successContentRepository.set(
                    heading = TextResource.Text(successPane.caption),
                    message = TextResource.Text(successPane.subCaption)
                )
            }
        }
    }
}
