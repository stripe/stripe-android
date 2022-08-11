package com.stripe.android.financialconnections.repository

import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.FinancialConnectionsSheet
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

    companion object {
        operator fun invoke(
            publishableKey: String,
            requestExecutor: FinancialConnectionsRequestExecutor,
            configuration: FinancialConnectionsSheet.Configuration,
            apiRequestFactory: ApiRequest.Factory,
            logger: Logger,
            initialManifest: FinancialConnectionsSessionManifest?
        ): FinancialConnectionsManifestRepository =
            FinancialConnectionsManifestRepositoryImpl(
                publishableKey,
                requestExecutor,
                configuration,
                apiRequestFactory,
                logger,
                initialManifest
            )
    }
}

private class FinancialConnectionsManifestRepositoryImpl(
    publishableKey: String,
    val requestExecutor: FinancialConnectionsRequestExecutor,
    val configuration: FinancialConnectionsSheet.Configuration,
    val apiRequestFactory: ApiRequest.Factory,
    val logger: Logger,
    initialManifest: FinancialConnectionsSessionManifest?
) : FinancialConnectionsManifestRepository {

    private val options = ApiRequest.Options(
        apiKey = publishableKey
    )

    /**
     * Ensures that manifest accesses via [getOrFetchManifest] suspend until
     * current writes are running.
     */
    val mutex = Mutex()
    private var cachedManifest: FinancialConnectionsSessionManifest? = initialManifest

    override suspend fun getOrFetchManifest(): FinancialConnectionsSessionManifest =
        mutex.withLock {
            cachedManifest ?: run {
                // fetch manifest and save it locally
                val financialConnectionsRequest = apiRequestFactory.createGet(
                    url = getManifestUrl,
                    options = options,
                    params = mapOf(
                        NetworkConstants.PARAMS_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret
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
            options = options,
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
            options = options,
            params = mapOf(
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret
            )
        )
        return requestExecutor.execute(
            financialConnectionsRequest,
            FinancialConnectionsSessionManifest.serializer()
        ).also { updateCachedManifest("consent acquired", it) }
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

        internal const val getManifestUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/manifest"

        internal const val consentAcquiredUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/consent_acquired"
    }
}
