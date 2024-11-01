package com.stripe.android.financialconnections.repository

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.domain.CachedPartnerAccount
import com.stripe.android.financialconnections.domain.toCachedPartnerAccounts
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.NetworkedAccountsList
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.model.ShareNetworkedAccountsResponse
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.network.NetworkConstants
import com.stripe.android.financialconnections.network.NetworkConstants.PARAMS_CLIENT_SECRET
import com.stripe.android.financialconnections.network.NetworkConstants.PARAMS_CONSUMER_CLIENT_SECRET
import com.stripe.android.financialconnections.network.NetworkConstants.PARAMS_ID
import com.stripe.android.financialconnections.network.NetworkConstants.PARAM_SELECTED_ACCOUNTS
import com.stripe.android.financialconnections.repository.api.ProvideApiRequestOptions
import com.stripe.android.financialconnections.utils.filterNotNullValues

/**
 * Repository to centralize reads and writes to the [FinancialConnectionsSessionManifest]
 * of the current flow.
 */
internal interface FinancialConnectionsAccountsRepository {

    suspend fun getCachedAccounts(): List<CachedPartnerAccount>?

    suspend fun updateCachedAccounts(partnerAccountsList: List<PartnerAccount>?)

    suspend fun postAuthorizationSessionAccounts(
        clientSecret: String,
        sessionId: String
    ): PartnerAccountsList

    /**
     * Retrieve the networked accounts for the Link consumer after verification
     *
     * This method is called after the account holder has been verified as an existing Link consumer.
     */
    suspend fun getNetworkedAccounts(
        clientSecret: String,
        consumerSessionClientSecret: String,
    ): NetworkedAccountsList

    suspend fun postAttachPaymentAccountToLinkAccountSession(
        clientSecret: String,
        paymentAccount: PaymentAccountParams,
        consumerSessionClientSecret: String?
    ): LinkAccountSessionPaymentAccount

    suspend fun postAuthorizationSessionSelectedAccounts(
        clientSecret: String,
        sessionId: String,
        selectAccounts: List<String>,
    ): PartnerAccountsList

    suspend fun postShareNetworkedAccounts(
        clientSecret: String,
        consumerSessionClientSecret: String,
        selectedAccountIds: Set<String>,
        consentAcquired: Boolean?
    ): ShareNetworkedAccountsResponse

    suspend fun pollAccountNumbers(linkedAccounts: Set<String>)

    companion object {
        operator fun invoke(
            requestExecutor: FinancialConnectionsRequestExecutor,
            provideApiRequestOptions: ProvideApiRequestOptions,
            apiRequestFactory: ApiRequest.Factory,
            logger: Logger,
            savedStateHandle: SavedStateHandle,
        ): FinancialConnectionsAccountsRepository =
            FinancialConnectionsAccountsRepositoryImpl(
                requestExecutor,
                provideApiRequestOptions,
                apiRequestFactory,
                logger,
                savedStateHandle,
            )
    }
}

private class FinancialConnectionsAccountsRepositoryImpl(
    private val requestExecutor: FinancialConnectionsRequestExecutor,
    private val provideApiRequestOptions: ProvideApiRequestOptions,
    private val apiRequestFactory: ApiRequest.Factory,
    private val logger: Logger,
    private val savedStateHandle: SavedStateHandle,
) : FinancialConnectionsAccountsRepository {

    override suspend fun getCachedAccounts(): List<CachedPartnerAccount>? {
        return savedStateHandle[CachedPartnerAccountsKey]
    }

    override suspend fun updateCachedAccounts(partnerAccountsList: List<PartnerAccount>?) {
        updateCachedAccounts(
            source = "updateCachedAccounts",
            accounts = partnerAccountsList.orEmpty(),
        )
    }

    override suspend fun postAuthorizationSessionAccounts(
        clientSecret: String,
        sessionId: String
    ): PartnerAccountsList {
        val request = apiRequestFactory.createPost(
            url = accountsSessionUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
            params = mapOf(
                PARAMS_ID to sessionId,
                PARAMS_CLIENT_SECRET to clientSecret,
                "expand" to listOf("data.institution"),
            )
        )
        return requestExecutor.execute(
            request,
            PartnerAccountsList.serializer()
        ).also {
            updateCachedAccounts("getOrFetchAccounts", it.data)
        }
    }

    override suspend fun getNetworkedAccounts(
        clientSecret: String,
        consumerSessionClientSecret: String
    ): NetworkedAccountsList {
        val request = apiRequestFactory.createGet(
            url = networkedAccountsUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
                PARAMS_CONSUMER_CLIENT_SECRET to consumerSessionClientSecret,
                "expand" to listOf("data.institution"),
            )
        )
        return requestExecutor.execute(
            request,
            NetworkedAccountsList.serializer()
        ).also {
            updateCachedAccounts("getNetworkedAccounts", it.data)
        }
    }

    override suspend fun postShareNetworkedAccounts(
        clientSecret: String,
        consumerSessionClientSecret: String,
        selectedAccountIds: Set<String>,
        consentAcquired: Boolean?
    ): ShareNetworkedAccountsResponse {
        val request = apiRequestFactory.createPost(
            url = shareNetworkedAccountsUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
                PARAMS_CONSUMER_CLIENT_SECRET to consumerSessionClientSecret,
                "consent_acquired" to consentAcquired,
            ).filterNotNullValues() + selectedAccountIds.mapIndexed { index, selectedAccountId ->
                "$PARAM_SELECTED_ACCOUNTS[$index]" to selectedAccountId
            },
        )
        return requestExecutor.execute(
            request,
            ShareNetworkedAccountsResponse.serializer()
        )
    }

    override suspend fun postAttachPaymentAccountToLinkAccountSession(
        clientSecret: String,
        paymentAccount: PaymentAccountParams,
        consumerSessionClientSecret: String?
    ): LinkAccountSessionPaymentAccount {
        val request = apiRequestFactory.createPost(
            url = attachPaymentAccountUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
            params = mapOf(
                PARAMS_CONSUMER_CLIENT_SECRET to consumerSessionClientSecret,
                PARAMS_CLIENT_SECRET to clientSecret
            ).filterNotNullValues() + paymentAccount.toParamMap()
        )
        return requestExecutor.execute(
            request,
            LinkAccountSessionPaymentAccount.serializer()
        )
    }

    override suspend fun postAuthorizationSessionSelectedAccounts(
        clientSecret: String,
        sessionId: String,
        selectAccounts: List<String>
    ): PartnerAccountsList {
        val request = apiRequestFactory.createPost(
            url = authorizationSessionSelectedAccountsUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
            params = mapOf(
                PARAMS_ID to sessionId,
                PARAMS_CLIENT_SECRET to clientSecret,
                "expand" to listOf("data.institution"),
            ) + selectAccounts.mapIndexed { index, account -> "$PARAM_SELECTED_ACCOUNTS[$index]" to account }
        )
        return requestExecutor.execute(
            request,
            PartnerAccountsList.serializer()
        ).also {
            updateCachedAccounts(
                "postAuthorizationSessionSelectedAccounts",
                it.data
            )
        }
    }

    override suspend fun pollAccountNumbers(linkedAccounts: Set<String>) {
        val accounts = linkedAccounts.mapIndexed { index, account ->
            "${NetworkConstants.PARAM_LINKED_ACCOUNTS}[$index]" to account
        }.toMap()

        val request = apiRequestFactory.createGet(
            url = pollAccountsNumbersUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = false),
            params = accounts,
        )

        requestExecutor.execute(request)
    }

    private fun updateCachedAccounts(
        source: String,
        accounts: List<PartnerAccount>
    ) {
        logger.debug("updating local partner accounts from $source")
        val cachedAccounts = accounts.toCachedPartnerAccounts()
        savedStateHandle[CachedPartnerAccountsKey] = cachedAccounts
    }

    companion object {
        private const val CachedPartnerAccountsKey = "CachedPartnerAccounts"

        internal const val accountsSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/accounts"

        internal const val networkedAccountsUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/networked_accounts"

        internal const val shareNetworkedAccountsUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/share_networked_account"

        internal const val attachPaymentAccountUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/attach_payment_account"

        internal const val authorizationSessionSelectedAccountsUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/selected_accounts"

        internal const val pollAccountsNumbersUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/poll_account_numbers"
    }
}
