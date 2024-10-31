package com.stripe.android.financialconnections.networking

import com.stripe.android.financialconnections.domain.CachedPartnerAccount
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.NetworkedAccountsList
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.model.ShareNetworkedAccountsResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository

internal abstract class AbsFinancialConnectionsAccountsRepository : FinancialConnectionsAccountsRepository {

    override suspend fun getCachedAccounts(): List<CachedPartnerAccount>? {
        TODO("Not yet implemented")
    }

    override suspend fun updateCachedAccounts(partnerAccountsList: List<PartnerAccount>?) {
        TODO("Not yet implemented")
    }

    override suspend fun postAuthorizationSessionAccounts(
        clientSecret: String,
        sessionId: String
    ): PartnerAccountsList {
        TODO("Not yet implemented")
    }

    override suspend fun getNetworkedAccounts(
        clientSecret: String,
        consumerSessionClientSecret: String
    ): NetworkedAccountsList {
        TODO("Not yet implemented")
    }

    override suspend fun postAttachPaymentAccountToLinkAccountSession(
        clientSecret: String,
        paymentAccount: PaymentAccountParams,
        consumerSessionClientSecret: String?
    ): LinkAccountSessionPaymentAccount {
        TODO("Not yet implemented")
    }

    override suspend fun postAuthorizationSessionSelectedAccounts(
        clientSecret: String,
        sessionId: String,
        selectAccounts: List<String>,
    ): PartnerAccountsList {
        TODO("Not yet implemented")
    }

    override suspend fun postShareNetworkedAccounts(
        clientSecret: String,
        consumerSessionClientSecret: String,
        selectedAccountIds: Set<String>,
        consentAcquired: Boolean?
    ): ShareNetworkedAccountsResponse {
        TODO("Not yet implemented")
    }

    override suspend fun pollAccountNumbers(linkedAccounts: Set<String>) {
        TODO("Not yet implemented")
    }
}
