package com.stripe.android.financialconnections.repository

import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.network.NetworkConstants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository to centralize reads and writes to the [FinancialConnectionsSessionManifest]
 * of the current flow.
 */
internal interface FinancialConnectionsManifestRepository {

    /**
     * Generates an initial [FinancialConnectionsSessionManifest] to start the AuthFlow, and
     * caches it locally.
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun generateFinancialConnectionsSessionManifest(
        clientSecret: String,
        applicationId: String
    ): FinancialConnectionsSessionManifest

    /**
     * Retrieves the current cached [FinancialConnectionsSessionManifest] instance, or fetches
     * it from backend if no cached version available.
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun getOrFetchManifest(): FinancialConnectionsSessionManifest

    /**
     * Marks the consent pane as completed, and caches the updated [FinancialConnectionsSessionManifest] object
     * received as result.
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun markConsentAcquired(
        clientSecret: String
    ): FinancialConnectionsSessionManifest

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun postAuthorizationSession(
        clientSecret: String,
        applicationId: String,
        institution: FinancialConnectionsInstitution,
    ): FinancialConnectionsAuthorizationSession

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun completeAuthorizationSession(
        clientSecret: String,
        sessionId: String,
        publicToken: String? = null
    ): FinancialConnectionsAuthorizationSession

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun postMarkLinkingMoreAccounts(
        clientSecret: String
    ): FinancialConnectionsSessionManifest

    suspend fun cancelAuthorizationSession(
        clientSecret: String,
        sessionId: String
    ): FinancialConnectionsAuthorizationSession

    companion object {
        operator fun invoke(
            requestExecutor: FinancialConnectionsRequestExecutor,
            configuration: FinancialConnectionsSheet.Configuration,
            apiRequestFactory: ApiRequest.Factory,
            apiOptions: ApiRequest.Options,
            logger: Logger
        ): FinancialConnectionsManifestRepository =
            FinancialConnectionsManifestRepositoryImpl(
                requestExecutor,
                configuration,
                apiRequestFactory,
                apiOptions,
                logger,
            )
    }

    fun updateLocalManifest(
        block: (FinancialConnectionsSessionManifest) -> FinancialConnectionsSessionManifest
    )
}

@Suppress("TooManyFunctions")
private class FinancialConnectionsManifestRepositoryImpl(
    val requestExecutor: FinancialConnectionsRequestExecutor,
    val configuration: FinancialConnectionsSheet.Configuration,
    val apiRequestFactory: ApiRequest.Factory,
    val apiOptions: ApiRequest.Options,
    val logger: Logger,
) : FinancialConnectionsManifestRepository {

    /**
     * Ensures that manifest accesses via [getOrFetchManifest] suspend until
     * current writes are running.
     */
    val mutex = Mutex()
    private var cachedManifest: FinancialConnectionsSessionManifest? = null

    override suspend fun getOrFetchManifest(): FinancialConnectionsSessionManifest =
        mutex.withLock {
            cachedManifest ?: run {
                // fetch manifest and save it locally
                val financialConnectionsRequest = apiRequestFactory.createGet(
                    url = getManifestUrl,
                    options = apiOptions,
                    params = mapOf(
                        NetworkConstants.PARAMS_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret,
                        "expand" to listOf("active_auth_session")
                    )
                )
                return requestExecutor.execute(
                    financialConnectionsRequest,
                    FinancialConnectionsSessionManifest.serializer()
                ).also { updateCachedManifest("get/fetch", it) }
            }
        }

    override suspend fun generateFinancialConnectionsSessionManifest(
        clientSecret: String,
        applicationId: String
    ): FinancialConnectionsSessionManifest = mutex.withLock {
        val financialConnectionsRequest = apiRequestFactory.createPost(
            url = generateHostedUrl,
            options = apiOptions,
            params = mapOf(
                PARAMS_FULLSCREEN to true,
                PARAMS_HIDE_CLOSE_BUTTON to true,
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret,
                NetworkConstants.PARAMS_APPLICATION_ID to applicationId
            )
        )
        return requestExecutor.execute(
            financialConnectionsRequest,
            FinancialConnectionsSessionManifest.serializer()
        ).also { updateCachedManifest("generating", it) }
    }

    override suspend fun markConsentAcquired(
        clientSecret: String
    ): FinancialConnectionsSessionManifest = mutex.withLock {
        val financialConnectionsRequest = apiRequestFactory.createPost(
            url = consentAcquiredUrl,
            options = apiOptions,
            params = mapOf(
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret
            )
        )
        return requestExecutor.execute(
            financialConnectionsRequest,
            FinancialConnectionsSessionManifest.serializer()
        ).also { updateCachedManifest("consent acquired", it) }
    }

    override suspend fun postAuthorizationSession(
        clientSecret: String,
        applicationId: String,
        institution: FinancialConnectionsInstitution
    ): FinancialConnectionsAuthorizationSession {
        val request = apiRequestFactory.createPost(
            url = FinancialConnectionsRepositoryImpl.authorizationSessionUrl,
            options = apiOptions,
            params = mapOf(
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret,
                "use_mobile_handoff" to false,
                "use_abstract_flow" to true,
                "return_url" to "auth-redirect/$applicationId",
                "institution" to institution.id
            )
        )
        return requestExecutor.execute(
            request,
            FinancialConnectionsAuthorizationSession.serializer()
        ).also {
            updateActiveInstitution("postAuthorizationSession", institution)
            updateCachedActiveAuthSession("postAuthorizationSession", it)
        }
    }

    override suspend fun cancelAuthorizationSession(
        clientSecret: String,
        sessionId: String
    ): FinancialConnectionsAuthorizationSession {
        val request = apiRequestFactory.createPost(
            url = cancelAuthSessionUrl,
            options = apiOptions,
            params = mapOf(
                NetworkConstants.PARAMS_ID to sessionId,
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret
            )
        )
        return requestExecutor.execute(
            request,
            FinancialConnectionsAuthorizationSession.serializer()
        ).also {
            updateCachedActiveAuthSession("cancelAuthorizationSession", it)
        }
    }

    override suspend fun completeAuthorizationSession(
        clientSecret: String,
        sessionId: String,
        publicToken: String?
    ): FinancialConnectionsAuthorizationSession {
        val request = apiRequestFactory.createPost(
            url = FinancialConnectionsRepositoryImpl.authorizeSessionUrl,
            options = apiOptions,
            params = mapOf(
                NetworkConstants.PARAMS_ID to sessionId,
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret,
                "public_token" to publicToken
            ).filter { it.value != null }
        )
        return requestExecutor.execute(
            request,
            FinancialConnectionsAuthorizationSession.serializer()
        ).also {
            updateCachedActiveAuthSession("completeAuthorizationSession", it)
        }
    }

    override suspend fun postMarkLinkingMoreAccounts(
        clientSecret: String
    ): FinancialConnectionsSessionManifest {
        val request = apiRequestFactory.createPost(
            url = linkMoreAccountsUrl,
            options = apiOptions,
            params = mapOf(
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret
            )
        )
        return requestExecutor.execute(
            request,
            FinancialConnectionsSessionManifest.serializer()
        ).also {
            updateCachedManifest("postMarkLinkingMoreAccounts", it)
        }
    }

    override fun updateLocalManifest(
        block: (FinancialConnectionsSessionManifest) -> FinancialConnectionsSessionManifest
    ) {
        cachedManifest
            ?.let { block(it) }
            ?.let { updateCachedManifest("updateLocalManifest", it) }
    }

    private fun updateActiveInstitution(
        source: String,
        institution: FinancialConnectionsInstitution
    ) {
        logger.debug("MANIFEST: updating local active institution from $source")
        cachedManifest = cachedManifest?.copy(
            activeInstitution = institution
        )
    }

    private fun updateCachedActiveAuthSession(
        source: String,
        authSession: FinancialConnectionsAuthorizationSession
    ) {
        logger.debug("MANIFEST: updating local active auth session from $source")
        cachedManifest = cachedManifest?.copy(
            activeAuthSession = authSession
        )
    }

    private fun updateCachedManifest(
        source: String,
        manifest: FinancialConnectionsSessionManifest
    ) {
        logger.debug("MANIFEST: updating local manifest from $source")
        cachedManifest = manifest
    }

    companion object {
        internal const val PARAMS_FULLSCREEN = "fullscreen"
        internal const val PARAMS_HIDE_CLOSE_BUTTON = "hide_close_button"

        internal const val generateHostedUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/generate_hosted_url"

        internal const val cancelAuthSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/cancel"

        internal const val getManifestUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/manifest"

        internal const val consentAcquiredUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/consent_acquired"

        internal const val linkMoreAccountsUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/link_more_accounts"
    }
}
