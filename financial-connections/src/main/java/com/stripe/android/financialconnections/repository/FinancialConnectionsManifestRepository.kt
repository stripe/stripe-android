package com.stripe.android.financialconnections.repository

import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.analytics.AuthSessionEvent
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.network.NetworkConstants
import com.stripe.android.financialconnections.network.NetworkConstants.PARAM_SELECTED_ACCOUNTS
import com.stripe.android.financialconnections.repository.api.ProvideApiRequestOptions
import com.stripe.android.financialconnections.utils.filterNotNullValues
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import java.util.Locale

/**
 * Repository to centralize reads and writes to the [FinancialConnectionsSessionManifest]
 * of the current flow.
 */
internal interface FinancialConnectionsManifestRepository {

    /**
     * Retrieves the current cached [SynchronizeSessionResponse] instance, or fetches
     * it from backend if no cached version available.
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun getOrSynchronizeFinancialConnectionsSession(
        clientSecret: String,
        applicationId: String,
        supportsAppVerification: Boolean,
        reFetchCondition: (SynchronizeSessionResponse) -> Boolean
    ): SynchronizeSessionResponse

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

    suspend fun postAuthorizationSessionEvent(
        clientSecret: String,
        clientTimestamp: Date,
        sessionId: String,
        authSessionEvents: List<AuthSessionEvent>
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

    suspend fun retrieveAuthorizationSession(
        clientSecret: String,
        sessionId: String
    ): FinancialConnectionsAuthorizationSession

    /**
     * Save the authorized bank accounts to Link.
     *
     * This method could be called on behalf of a Link consumer or non-Link consumer.
     *
     * @return [FinancialConnectionsSessionManifest]
     */
    suspend fun postSaveAccountsToLink(
        clientSecret: String,
        email: String?,
        country: String?,
        locale: String?,
        phoneNumber: String?,
        consumerSessionClientSecret: String?,
        selectedAccounts: Set<String>?
    ): FinancialConnectionsSessionManifest

    /**
     * Disable networking in Connections Auth Flow
     *
     * Disable networking in Connections Auth Flow when the user chooses to continue in guest mode.
     *
     * @return [FinancialConnectionsSessionManifest]
     */
    suspend fun disableNetworking(
        clientSecret: String,
        disabledReason: String?,
        clientSuggestedNextPaneOnDisableNetworking: String?
    ): FinancialConnectionsSessionManifest

    /**
     * Mark when the user has verified (logged in) via SMS OTP to their Link account in the networking auth flow
     *
     * When the user verifies via SMS OTP and logs in to their Link account in
     * the networking auth flow, mark it on the link account session so we don't ask them to log in again.
     *
     * @return [FinancialConnectionsSessionManifest]
     */
    suspend fun postMarkLinkVerified(
        clientSecret: String,
    ): FinancialConnectionsSessionManifest

    /**
     * Mark when the user has verified (logged in) via email OTP as a step up authentication
     * of SMS OTP to their Link account in the networking auth flow
     *
     * When the user verifies via email OTP in the networking auth flow,
     * mark it on the link account session.
     *
     * @return [FinancialConnectionsSessionManifest]
     */
    suspend fun postMarkLinkStepUpVerified(
        clientSecret: String,
    ): FinancialConnectionsSessionManifest

    fun updateLocalManifest(
        block: (FinancialConnectionsSessionManifest) -> FinancialConnectionsSessionManifest
    )

    companion object {
        operator fun invoke(
            requestExecutor: FinancialConnectionsRequestExecutor,
            apiRequestFactory: ApiRequest.Factory,
            provideApiRequestOptions: ProvideApiRequestOptions,
            logger: Logger,
            locale: Locale,
            initialSync: SynchronizeSessionResponse?
        ): FinancialConnectionsManifestRepository =
            FinancialConnectionsManifestRepositoryImpl(
                requestExecutor,
                apiRequestFactory,
                provideApiRequestOptions,
                locale,
                logger,
                initialSync
            )
    }
}

private class FinancialConnectionsManifestRepositoryImpl(
    val requestExecutor: FinancialConnectionsRequestExecutor,
    val apiRequestFactory: ApiRequest.Factory,
    val provideApiRequestOptions: ProvideApiRequestOptions,
    val locale: Locale,
    val logger: Logger,
    initialSync: SynchronizeSessionResponse?
) : FinancialConnectionsManifestRepository {

    /**
     * Ensures that manifest accesses via [getOrSynchronizeFinancialConnectionsSession] suspend until
     * current writes are running.
     */
    val mutex = Mutex()
    private var cachedSynchronizeSessionResponse: SynchronizeSessionResponse? = initialSync

    override suspend fun getOrSynchronizeFinancialConnectionsSession(
        clientSecret: String,
        applicationId: String,
        supportsAppVerification: Boolean,
        reFetchCondition: (SynchronizeSessionResponse) -> Boolean
    ): SynchronizeSessionResponse = mutex.withLock {
        val cachedSync = cachedSynchronizeSessionResponse?.takeUnless(reFetchCondition)
        return cachedSync ?: synchronize(applicationId, clientSecret, supportsAppVerification)
    }

    private suspend fun synchronize(
        applicationId: String,
        clientSecret: String,
        supportsAppVerification: Boolean,
    ): SynchronizeSessionResponse = requestExecutor.execute(
        apiRequestFactory.createPost(
            url = synchronizeSessionUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
            params = mapOf(
                "expand" to listOf("manifest.active_auth_session"),
                "emit_events" to true,
                "locale" to locale.toLanguageTag(),
                "mobile" to mapOf(
                    // This ensures backend returns v3 responses.
                    "forced_authflow_version" to "v3",
                    PARAMS_FULLSCREEN to true,
                    PARAMS_HIDE_CLOSE_BUTTON to true,
                    PARAMS_SUPPORT_APP_VERIFICATION to supportsAppVerification,
                    PARAMS_VERIFY_APP_ID to applicationId,
                    NetworkConstants.PARAMS_APPLICATION_ID to applicationId
                ),
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret
            )
        ),
        SynchronizeSessionResponse.serializer()
    ).also { updateCachedSynchronizeSessionResponse("get/fetch", it) }

    override suspend fun markConsentAcquired(
        clientSecret: String
    ): FinancialConnectionsSessionManifest = mutex.withLock {
        val financialConnectionsRequest = apiRequestFactory.createPost(
            url = consentAcquiredUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = false),
            params = mapOf(
                "expand" to listOf("active_auth_session"),
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
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
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

    override suspend fun postAuthorizationSessionEvent(
        clientSecret: String,
        clientTimestamp: Date,
        sessionId: String,
        authSessionEvents: List<AuthSessionEvent>
    ): FinancialConnectionsAuthorizationSession {
        val request = apiRequestFactory.createPost(
            url = eventsAuthSessionUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
            params = mapOf(
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret,
                "client_timestamp" to clientTimestamp.time.toString(),
                NetworkConstants.PARAMS_ID to sessionId,
            ) + authSessionEvents.mapIndexed { index, event ->
                "frontend_events[$index]" to event.toMap()
            }
        )
        return requestExecutor.execute(
            request,
            FinancialConnectionsAuthorizationSession.serializer()
        )
    }

    override suspend fun cancelAuthorizationSession(
        clientSecret: String,
        sessionId: String
    ): FinancialConnectionsAuthorizationSession {
        val request = apiRequestFactory.createPost(
            url = cancelAuthSessionUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
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

    override suspend fun retrieveAuthorizationSession(
        clientSecret: String,
        sessionId: String
    ): FinancialConnectionsAuthorizationSession = requestExecutor.execute(
        request = apiRequestFactory.createPost(
            url = retrieveAuthSessionUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
            params = mapOf(
                NetworkConstants.PARAMS_ID to sessionId,
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret,
                "emit_events" to true
            )
        ),
        FinancialConnectionsAuthorizationSession.serializer()
    ).also {
        updateCachedActiveAuthSession("retrieveAuthorizationSession", it)
    }

    override suspend fun completeAuthorizationSession(
        clientSecret: String,
        sessionId: String,
        publicToken: String?
    ): FinancialConnectionsAuthorizationSession {
        val request = apiRequestFactory.createPost(
            url = FinancialConnectionsRepositoryImpl.authorizeSessionUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
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
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
            params = mapOf(
                "expand" to listOf("active_auth_session"),
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

    override suspend fun postSaveAccountsToLink(
        clientSecret: String,
        email: String?,
        country: String?,
        locale: String?,
        phoneNumber: String?,
        consumerSessionClientSecret: String?,
        selectedAccounts: Set<String>?,
    ): FinancialConnectionsSessionManifest {
        // Accounts to be saved can be null in case of manual entry.
        val accounts: Map<String, Any> = selectedAccounts
            ?.mapIndexed { index, account -> "$PARAM_SELECTED_ACCOUNTS[$index]" to account }
            ?.toMap()
            ?: emptyMap()
        val request = apiRequestFactory.createPost(
            url = saveAccountToLinkUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = false),
            params = mapOf(
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret,
                NetworkConstants.PARAMS_CONSUMER_CLIENT_SECRET to consumerSessionClientSecret,
                "expand" to listOf("active_auth_session"),
                "country" to country,
                "locale" to locale,
                "email_address" to email,
                "phone_number" to phoneNumber
            ).filterNotNullValues() + accounts
        )
        return requestExecutor.execute(
            request,
            FinancialConnectionsSessionManifest.serializer()
        ).also {
            updateCachedManifest("postSaveAccountsToLink", it)
        }
    }

    override suspend fun disableNetworking(
        clientSecret: String,
        disabledReason: String?,
        clientSuggestedNextPaneOnDisableNetworking: String?
    ): FinancialConnectionsSessionManifest {
        val request = apiRequestFactory.createPost(
            url = disableNetworking,
            options = provideApiRequestOptions(useConsumerPublishableKey = false),
            params = mapOf(
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret,
                "expand" to listOf("active_auth_session"),
                "client_requested_next_pane_on_disable_networking" to clientSuggestedNextPaneOnDisableNetworking,
                "disabled_reason" to disabledReason,
            ).filterNotNullValues()
        )
        return requestExecutor.execute(
            request,
            FinancialConnectionsSessionManifest.serializer()
        ).also {
            updateCachedManifest("postSaveAccountsToLink", it)
        }
    }

    override suspend fun postMarkLinkVerified(
        clientSecret: String
    ): FinancialConnectionsSessionManifest {
        val request = apiRequestFactory.createPost(
            url = linkVerifiedUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = false),
            params = mapOf(
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret,
                "expand" to listOf("active_auth_session"),
            )
        )
        return requestExecutor.execute(
            request,
            FinancialConnectionsSessionManifest.serializer()
        ).also {
            updateCachedManifest("postMarkLinkVerified", it)
        }
    }

    override suspend fun postMarkLinkStepUpVerified(
        clientSecret: String
    ): FinancialConnectionsSessionManifest {
        val request = apiRequestFactory.createPost(
            url = linkStepUpVerifiedUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = false),
            params = mapOf(
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret,
                "expand" to listOf("active_auth_session"),
            )
        )
        return requestExecutor.execute(
            request,
            FinancialConnectionsSessionManifest.serializer()
        ).also {
            updateCachedManifest("postMarkLinkStepUpVerified", it)
        }
    }

    override fun updateLocalManifest(
        block: (FinancialConnectionsSessionManifest) -> FinancialConnectionsSessionManifest
    ) {
        cachedSynchronizeSessionResponse?.manifest
            ?.let { block(it) }
            ?.let { updateCachedManifest("updateLocalManifest", it) }
    }

    private fun updateActiveInstitution(
        source: String,
        institution: FinancialConnectionsInstitution
    ) {
        logger.debug("SYNC_CACHE: updating local active institution from $source")
        cachedSynchronizeSessionResponse?.manifest?.copy(activeInstitution = institution)
            ?.let { updateCachedManifest("updating active institution", it) }
    }

    private fun updateCachedActiveAuthSession(
        source: String,
        authSession: FinancialConnectionsAuthorizationSession
    ) {
        logger.debug("SYNC_CACHE: updating local active auth session from $source")
        cachedSynchronizeSessionResponse?.manifest?.copy(activeAuthSession = authSession)
            ?.let { updateCachedManifest("updating active auth session", it) }
    }

    private fun updateCachedSynchronizeSessionResponse(
        source: String,
        synchronizeSessionResponse: SynchronizeSessionResponse
    ) {
        logger.debug("SYNC_CACHE: updating local sync object from $source")
        cachedSynchronizeSessionResponse = synchronizeSessionResponse
    }

    private fun updateCachedManifest(
        source: String,
        manifest: FinancialConnectionsSessionManifest
    ) {
        logger.debug("SYNC_CACHE: updating local manifest from $source")
        cachedSynchronizeSessionResponse = cachedSynchronizeSessionResponse?.copy(
            manifest = manifest
        )
    }

    companion object {
        internal const val PARAMS_FULLSCREEN = "fullscreen"
        internal const val PARAMS_HIDE_CLOSE_BUTTON = "hide_close_button"
        internal const val PARAMS_SUPPORT_APP_VERIFICATION = "supports_app_verification"
        internal const val PARAMS_VERIFY_APP_ID = "verified_app_id"

        internal const val synchronizeSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/financial_connections/sessions/synchronize"

        internal const val cancelAuthSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/cancel"

        internal const val retrieveAuthSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/retrieve"

        internal const val eventsAuthSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/events"

        internal const val consentAcquiredUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/consent_acquired"

        internal const val linkMoreAccountsUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/link_more_accounts"

        internal const val saveAccountToLinkUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/save_accounts_to_link"

        internal const val linkVerifiedUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/link_verified"

        internal const val linkStepUpVerifiedUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/link_step_up_authentication_verified"

        internal const val disableNetworking: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/disable_networking"
    }
}
