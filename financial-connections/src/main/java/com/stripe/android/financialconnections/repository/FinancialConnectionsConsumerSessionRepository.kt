package com.stripe.android.financialconnections.repository

import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.repository.api.FinancialConnectionsConsumersApiService
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.CustomEmailType
import com.stripe.android.model.VerificationType
import com.stripe.android.repository.ConsumersApiService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

internal interface FinancialConnectionsConsumerSessionRepository {

    suspend fun getCachedConsumerSession(): ConsumerSession?
    suspend fun lookupConsumerSession(
        email: String,
        clientSecret: String
    ): ConsumerSessionLookup

    suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
        connectionsMerchantName: String?,
        type: VerificationType,
        customEmailType: CustomEmailType?
    ): ConsumerSession

    suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        type: VerificationType,
    ): ConsumerSession

    companion object {
        operator fun invoke(
            consumersApiService: ConsumersApiService,
            apiOptions: ApiRequest.Options,
            financialConnectionsConsumersApiService: FinancialConnectionsConsumersApiService,
            locale: Locale?,
            logger: Logger,
        ): FinancialConnectionsConsumerSessionRepository =
            FinancialConnectionsConsumerSessionRepositoryImpl(
                consumersApiService = consumersApiService,
                apiOptions = apiOptions,
                financialConnectionsConsumersApiService = financialConnectionsConsumersApiService,
                locale = locale,
                logger = logger,
            )
    }
}

private class FinancialConnectionsConsumerSessionRepositoryImpl(
    private val financialConnectionsConsumersApiService: FinancialConnectionsConsumersApiService,
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
        email: String,
        clientSecret: String
    ): ConsumerSessionLookup = mutex.withLock {
        postConsumerSession(email, clientSecret).also { lookup ->
            updateCachedConsumerSession("lookupConsumerSession", lookup.consumerSession)
        }
    }

    override suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
        connectionsMerchantName: String?,
        type: VerificationType,
        customEmailType: CustomEmailType?,
    ): ConsumerSession = mutex.withLock {
        consumersApiService.startConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            locale = locale ?: Locale.getDefault(),
            authSessionCookie = null,
            connectionsMerchantName = connectionsMerchantName,
            requestSurface = CONSUMER_SURFACE,
            type = type,
            customEmailType = customEmailType,
            requestOptions = apiOptions
        ).also { session ->
            updateCachedConsumerSession("startConsumerVerification", session)
        }
    }

    override suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        type: VerificationType
    ): ConsumerSession = mutex.withLock {
        return consumersApiService.confirmConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            authSessionCookie = null,
            verificationCode = verificationCode,
            type = type,
            requestSurface = CONSUMER_SURFACE,
            requestOptions = apiOptions
        ).also { session ->
            updateCachedConsumerSession("confirmConsumerVerification", session)
        }
    }

    private suspend fun postConsumerSession(
        email: String,
        clientSecret: String
    ): ConsumerSessionLookup = financialConnectionsConsumersApiService.postConsumerSession(
        email = email,
        clientSecret = clientSecret,
        requestSurface = CONSUMER_SURFACE,
    )

    private fun updateCachedConsumerSession(
        source: String,
        consumerSession: ConsumerSession?
    ) {
        logger.debug("SYNC_CACHE: updating local consumer session from $source")
        cachedConsumerSession = consumerSession
    }

    private companion object {
        private const val CONSUMER_SURFACE: String = "android_connections"
    }
}
