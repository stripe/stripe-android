package com.stripe.android.financialconnections.repository

import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.network.NetworkConstants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository to centralize reads and writes to the [FinancialConnectionsSessionManifest]
 * of the current flow.
 */
internal interface FinancialConnectionsAccountsRepository {

    suspend fun postAuthorizationSessionAccounts(
        clientSecret: String,
        sessionId: String
    ): PartnerAccountsList

    suspend fun postAuthorizationSessionSelectedAccounts(
        clientSecret: String,
        sessionId: String,
        selectAccounts: List<String>
    ): PartnerAccountsList

    companion object {
        operator fun invoke(
            publishableKey: String,
            requestExecutor: FinancialConnectionsRequestExecutor,
            apiRequestFactory: ApiRequest.Factory,
            logger: Logger
        ): FinancialConnectionsAccountsRepository =
            FinancialConnectionsAccountsRepositoryImpl(
                publishableKey,
                requestExecutor,
                apiRequestFactory,
                logger
            )
    }

    suspend fun getOrFetchAccounts(clientSecret: String, sessionId: String): PartnerAccountsList
}

private class FinancialConnectionsAccountsRepositoryImpl(
    publishableKey: String,
    val requestExecutor: FinancialConnectionsRequestExecutor,
    val apiRequestFactory: ApiRequest.Factory,
    val logger: Logger
) : FinancialConnectionsAccountsRepository {

    private val options = ApiRequest.Options(
        apiKey = publishableKey
    )

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
            options = options,
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

    override suspend fun postAuthorizationSessionSelectedAccounts(
        clientSecret: String,
        sessionId: String,
        selectAccounts: List<String>
    ): PartnerAccountsList {
        val request = apiRequestFactory.createPost(
            url = authorizationSessionSelectedAccountsUrl,
            options = options,
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

    private fun updateCachedAccounts(
        source: String,
        manifest: PartnerAccountsList
    ) {
        logger.debug("MANIFEST: updating local manifest from $source")
        cachedAccounts = manifest
    }

    companion object {
        internal const val PARAM_SELECTED_ACCOUNTS: String = "selected_accounts"

        internal const val accountsSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/accounts"

        internal const val authorizationSessionSelectedAccountsUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/selected_accounts"
    }
}
