package com.stripe.android.financialconnections.repository

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.repository.api.FinancialConnectionsConsumersApiService
import com.stripe.android.financialconnections.repository.api.ProvideApiRequestOptions
import com.stripe.android.model.AttachConsumerToLinkAccountSession
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.ConsumerSignUpConsentAction.EnteredPhoneNumberClickedSaveToLink
import com.stripe.android.model.CustomEmailType
import com.stripe.android.model.VerificationType
import com.stripe.android.repository.ConsumersApiService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

internal interface FinancialConnectionsConsumerSessionRepository {

    suspend fun getCachedConsumerSession(): CachedConsumerSession?

    suspend fun signUp(
        email: String,
        phoneNumber: String,
        country: String,
    ): ConsumerSessionSignup

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

    suspend fun attachLinkConsumerToLinkAccountSession(
        consumerSessionClientSecret: String,
        clientSecret: String,
    ): AttachConsumerToLinkAccountSession

    companion object {
        operator fun invoke(
            consumersApiService: ConsumersApiService,
            provideApiRequestOptions: ProvideApiRequestOptions,
            consumerSessionRepository: ConsumerSessionRepository,
            financialConnectionsConsumersApiService: FinancialConnectionsConsumersApiService,
            locale: Locale?,
            logger: Logger,
        ): FinancialConnectionsConsumerSessionRepository =
            FinancialConnectionsConsumerSessionRepositoryImpl(
                consumersApiService = consumersApiService,
                provideApiRequestOptions = provideApiRequestOptions,
                financialConnectionsConsumersApiService = financialConnectionsConsumersApiService,
                consumerSessionRepository = consumerSessionRepository,
                locale = locale,
                logger = logger,
            )
    }
}

private class FinancialConnectionsConsumerSessionRepositoryImpl(
    private val financialConnectionsConsumersApiService: FinancialConnectionsConsumersApiService,
    private val consumersApiService: ConsumersApiService,
    private val consumerSessionRepository: ConsumerSessionRepository,
    private val provideApiRequestOptions: ProvideApiRequestOptions,
    private val locale: Locale?,
    private val logger: Logger,
) : FinancialConnectionsConsumerSessionRepository {

    private val mutex = Mutex()

    override suspend fun getCachedConsumerSession(): CachedConsumerSession? = mutex.withLock {
        consumerSessionRepository.provideConsumerSession()
    }

    override suspend fun lookupConsumerSession(
        email: String,
        clientSecret: String
    ): ConsumerSessionLookup = mutex.withLock {
        postConsumerSession(email, clientSecret).also { lookup ->
            updateCachedConsumerSessionFromLookup(lookup)
        }
    }

    override suspend fun signUp(
        email: String,
        phoneNumber: String,
        country: String,
    ): ConsumerSessionSignup = mutex.withLock {
        consumersApiService.signUp(
            email = email,
            phoneNumber = phoneNumber,
            country = country,
            name = null,
            locale = locale,
            requestOptions = provideApiRequestOptions(useConsumerPublishableKey = false),
            requestSurface = CONSUMER_SURFACE,
            consentAction = EnteredPhoneNumberClickedSaveToLink,
        ).onSuccess { signup ->
            updateCachedConsumerSessionFromSignup(signup)
        }.getOrThrow()
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
            connectionsMerchantName = connectionsMerchantName,
            requestSurface = CONSUMER_SURFACE,
            type = type,
            customEmailType = customEmailType,
            requestOptions = provideApiRequestOptions(useConsumerPublishableKey = false),
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
            verificationCode = verificationCode,
            type = type,
            requestSurface = CONSUMER_SURFACE,
            requestOptions = provideApiRequestOptions(useConsumerPublishableKey = false),
        ).also { session ->
            updateCachedConsumerSession("confirmConsumerVerification", session)
        }
    }

    override suspend fun attachLinkConsumerToLinkAccountSession(
        consumerSessionClientSecret: String,
        clientSecret: String,
    ): AttachConsumerToLinkAccountSession {
        return consumersApiService.attachLinkConsumerToLinkAccountSession(
            consumerSessionClientSecret = consumerSessionClientSecret,
            clientSecret = clientSecret,
            requestSurface = CONSUMER_SURFACE,
            requestOptions = provideApiRequestOptions(useConsumerPublishableKey = false),
        )
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
        consumerSession: ConsumerSession,
    ) {
        logger.debug("SYNC_CACHE: updating local consumer session from $source")
        consumerSessionRepository.updateConsumerSession(consumerSession)
    }

    private fun updateCachedConsumerSessionFromLookup(
        lookup: ConsumerSessionLookup,
    ) {
        logger.debug("SYNC_CACHE: updating local consumer session from lookupConsumerSession")
        consumerSessionRepository.storeNewConsumerSession(lookup.consumerSession, lookup.publishableKey)
    }

    private fun updateCachedConsumerSessionFromSignup(
        signup: ConsumerSessionSignup,
    ) {
        logger.debug("SYNC_CACHE: updating local consumer session from signUp")
        consumerSessionRepository.storeNewConsumerSession(signup.consumerSession, signup.publishableKey)
    }

    private companion object {
        private const val CONSUMER_SURFACE: String = "android_connections"
    }
}
