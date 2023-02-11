package com.stripe.android.financialconnections.repository

import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.repository.ConsumersApiService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

interface FinancialConnectionsConsumerSessionRepository {

    suspend fun getCachedConsumerSession(): ConsumerSession?
    suspend fun lookupConsumerSession(email: String?): ConsumerSessionLookup
    suspend fun startConsumerVerification(consumerSessionClientSecret: String): ConsumerSession

    companion object {
        operator fun invoke(
            consumersApiService: ConsumersApiService,
            apiOptions: ApiRequest.Options,
            locale: Locale?,
            logger: Logger,
        ): FinancialConnectionsConsumerSessionRepository =
            FinancialConnectionsConsumerSessionRepositoryImpl(
                consumersApiService,
                apiOptions,
                locale,
                logger,
            )
    }
}

private class FinancialConnectionsConsumerSessionRepositoryImpl(
    private val consumersApiService: ConsumersApiService,
    private val apiOptions: ApiRequest.Options,
    private val locale: Locale?,
    private val logger: Logger,
) : FinancialConnectionsConsumerSessionRepository {

    private val mutex = Mutex()
    private var cachedConsumerSession: ConsumerSession? = null

    override suspend fun getCachedConsumerSession(): ConsumerSession? =
        mutex.withLock { cachedConsumerSession }

    override suspend fun lookupConsumerSession(
        email: String?,
    ): ConsumerSessionLookup = mutex.withLock {
        lookupRequest(email).also { lookup ->
            updateCachedConsumerSession("lookupConsumerSession", lookup.consumerSession)
        }
    }

    override suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
    ): ConsumerSession = mutex.withLock {
        consumersApiService.startConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            locale = locale ?: Locale.getDefault(),
            authSessionCookie = null,
            requestSurface = CONSUMER_SURFACE,
            requestOptions = apiOptions
        ).also { session ->
            updateCachedConsumerSession("startConsumerVerification", session)
        }
    }

    private suspend fun lookupRequest(email: String?): ConsumerSessionLookup =
        consumersApiService.lookupConsumerSession(
            email = email,
            authSessionCookie = null,
            requestSurface = CONSUMER_SURFACE,
            requestOptions = apiOptions
        )

    private fun updateCachedConsumerSession(
        source: String,
        consumerSession: ConsumerSession?
    ) {
        logger.debug("SYNC_CACHE: updating local consumer session from $source")
        cachedConsumerSession = consumerSession
    }

    private companion object {
        // TODO@carlosmuvi update consumer surface to be android specific.
        private const val CONSUMER_SURFACE: String = "web_connections"
    }
}
