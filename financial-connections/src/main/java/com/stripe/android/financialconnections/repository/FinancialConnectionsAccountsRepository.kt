package com.stripe.android.financialconnections.repository

import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.network.NetworkConstants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository to centralize reads and writes to the [FinancialConnectionsSessionManifest]
 * of the current flow.
 */
internal interface FinancialConnectionsAccountsRepository {

    suspend fun getOrFetchAccounts(
        clientSecret: String,
        sessionId: String
    ): PartnerAccountsList

    suspend fun postAuthorizationSessionAccounts(
        clientSecret: String,
        sessionId: String
    ): PartnerAccountsList

    suspend fun postLinkAccountSessionPaymentAccount(
        clientSecret: String,
        paymentAccount: PaymentAccountParams,
        consumerSessionClientSecret: String? = null
    ): LinkAccountSessionPaymentAccount

    suspend fun postAuthorizationSessionSelectedAccounts(
        clientSecret: String,
        sessionId: String,
        selectAccounts: List<String>
    ): PartnerAccountsList

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
     * Ensures that manifest accesses via [getOrFetchManifest] suspend until
     * current writes are running.
     */
    val mutex = Mutex()
    private var cachedAccounts: PartnerAccountsList? = null

    override suspend fun getOrFetchAccounts(
        clientSecret: String,
        sessionId: String,
    ): PartnerAccountsList =
        mutex.withLock {
            cachedAccounts ?: run {
                postAuthorizationSessionAccounts(clientSecret, sessionId)
            }
        }

    override suspend fun postAuthorizationSessionAccounts(
        clientSecret: String,
        sessionId: String,
    ): PartnerAccountsList {
        val request = apiRequestFactory.createPost(
            url = accountsSessionUrl,
            options = apiOptions,
            params = mapOf(
                NetworkConstants.PARAMS_ID to sessionId,
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret,
            )
        )
        return requestExecutor.execute(
            request,
            PartnerAccountsList.serializer()
        ).also {
            updateCachedAccounts("getOrFetchAccounts", it)
        }
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
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret
            ) + paymentAccount.toParamMap()
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
            options = apiOptions,
            params = mapOf(
                NetworkConstants.PARAMS_ID to sessionId,
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret,
            ) + selectAccounts.mapIndexed { index, account -> "$PARAM_SELECTED_ACCOUNTS[$index]" to account }
        )
        return requestExecutor.execute(
            request,
            PartnerAccountsList.serializer()
        ).also {
            updateCachedAccounts("postAuthorizationSessionSelectedAccounts", it)
        }
    }

    private suspend fun updateCachedAccounts(
        source: String,
        accounts: PartnerAccountsList
    ) = mutex.withLock {
        logger.debug("updating local partner accounts from $source")
        cachedAccounts = accounts
    }

    companion object {
        internal const val PARAM_SELECTED_ACCOUNTS: String = "selected_accounts"

        internal const val accountsSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/accounts"

        internal const val attachPaymentAccountUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/attach_payment_account"

        internal const val authorizationSessionSelectedAccountsUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/selected_accounts"
    }
}
