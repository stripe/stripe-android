package com.stripe.android.financialconnections.repository

import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.NetworkedAccountsList
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.network.NetworkConstants.PARAMS_CLIENT_SECRET
import com.stripe.android.financialconnections.network.NetworkConstants.PARAMS_CONSUMER_CLIENT_SECRET
import com.stripe.android.financialconnections.network.NetworkConstants.PARAMS_ID
import com.stripe.android.financialconnections.network.NetworkConstants.PARAM_SELECTED_ACCOUNTS
import com.stripe.android.financialconnections.utils.filterNotNullValues
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository to centralize reads and writes to the [FinancialConnectionsSessionManifest]
 * of the current flow.
 */
internal interface FinancialConnectionsAccountsRepository {

    suspend fun getCachedAccounts(): List<PartnerAccount>?

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

    suspend fun postLinkAccountSessionPaymentAccount(
        clientSecret: String,
        paymentAccount: PaymentAccountParams,
        consumerSessionClientSecret: String?
    ): LinkAccountSessionPaymentAccount

    suspend fun postAuthorizationSessionSelectedAccounts(
        clientSecret: String,
        sessionId: String,
        selectAccounts: List<String>,
        updateLocalCache: Boolean
    ): PartnerAccountsList

    suspend fun postShareNetworkedAccount(
        clientSecret: String,
        consumerSessionClientSecret: String,
        selectedAccountId: String
    ): InstitutionResponse

    companion object {
        operator fun invoke(
            requestExecutor: FinancialConnectionsRequestExecutor,
            apiRequestFactory: ApiRequest.Factory,
            apiOptions: ApiRequest.Options,
            logger: Logger
        ): FinancialConnectionsAccountsRepository =
            FinancialConnectionsAccountsRepositoryImpl(
                requestExecutor,
                apiRequestFactory,
                apiOptions,
                logger
            )
    }
}

private class FinancialConnectionsAccountsRepositoryImpl(
    val requestExecutor: FinancialConnectionsRequestExecutor,
    val apiRequestFactory: ApiRequest.Factory,
    val apiOptions: ApiRequest.Options,
    val logger: Logger
) : FinancialConnectionsAccountsRepository {

    /**
     * Ensures that [cachedAccounts] accesses via [getCachedAccounts] suspend until
     * current writes are running.
     */
    val mutex = Mutex()
    private var cachedAccounts: List<PartnerAccount>? = null

    override suspend fun getCachedAccounts(): List<PartnerAccount>? =
        mutex.withLock { cachedAccounts }

    override suspend fun updateCachedAccounts(partnerAccountsList: List<PartnerAccount>?) =
        mutex.withLock { cachedAccounts = partnerAccountsList }

    override suspend fun postAuthorizationSessionAccounts(
        clientSecret: String,
        sessionId: String
    ): PartnerAccountsList {
        val request = apiRequestFactory.createPost(
            url = accountsSessionUrl,
            options = apiOptions,
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
            options = apiOptions,
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

    override suspend fun postShareNetworkedAccount(
        clientSecret: String,
        consumerSessionClientSecret: String,
        selectedAccountId: String
    ): InstitutionResponse {
        val request = apiRequestFactory.createPost(
            url = shareNetworkedAccountsUrl,
            options = apiOptions,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
                PARAMS_CONSUMER_CLIENT_SECRET to consumerSessionClientSecret,
                "$PARAM_SELECTED_ACCOUNTS[0]" to selectedAccountId,
            )
        )
        return requestExecutor.execute(
            request,
            InstitutionResponse.serializer()
        )
    }

    override suspend fun postLinkAccountSessionPaymentAccount(
        clientSecret: String,
        paymentAccount: PaymentAccountParams,
        consumerSessionClientSecret: String?
    ): LinkAccountSessionPaymentAccount {
        val request = apiRequestFactory.createPost(
            url = attachPaymentAccountUrl,
            options = apiOptions,
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
        selectAccounts: List<String>,
        updateLocalCache: Boolean
    ): PartnerAccountsList {
        val request = apiRequestFactory.createPost(
            url = authorizationSessionSelectedAccountsUrl,
            options = apiOptions,
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
            if (updateLocalCache) {
                updateCachedAccounts(
                    "postAuthorizationSessionSelectedAccounts",
                    it.data
                )
            }
        }
    }

    private fun updateCachedAccounts(
        source: String,
        accounts: List<PartnerAccount>
    ) {
        logger.debug("updating local partner accounts from $source")
        cachedAccounts = accounts
    }

    companion object {
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
    }
}
