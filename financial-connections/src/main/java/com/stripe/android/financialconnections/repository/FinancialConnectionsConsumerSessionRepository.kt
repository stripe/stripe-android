package com.stripe.android.financialconnections.repository

import com.stripe.android.core.Logger
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext.BillingDetails
import com.stripe.android.financialconnections.domain.IsLinkWithStripe
import com.stripe.android.financialconnections.repository.api.FinancialConnectionsConsumersApiService
import com.stripe.android.financialconnections.repository.api.ProvideApiRequestOptions
import com.stripe.android.financialconnections.utils.toConsumerBillingAddressParams
import com.stripe.android.model.AttachConsumerToLinkAccountSession
import com.stripe.android.model.ConsumerFinancialIncentive
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.ConsumerSignUpConsentAction.EnteredPhoneNumberClickedSaveToLink
import com.stripe.android.model.CustomEmailType
import com.stripe.android.model.SharePaymentDetails
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

    suspend fun createPaymentDetails(
        bankAccountId: String,
        consumerSessionClientSecret: String,
        billingDetails: BillingDetails?,
    ): ConsumerPaymentDetails

    suspend fun updateIncentiveEligibility(
        paymentDetailId: String,
        paymentIntentId: String?,
        setupIntentId: String?,
    ): ConsumerFinancialIncentive

    suspend fun sharePaymentDetails(
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
        expectedPaymentMethodType: String,
        billingPhone: String?,
    ): SharePaymentDetails

    companion object {
        operator fun invoke(
            consumersApiService: ConsumersApiService,
            provideApiRequestOptions: ProvideApiRequestOptions,
            consumerSessionRepository: ConsumerSessionRepository,
            financialConnectionsConsumersApiService: FinancialConnectionsConsumersApiService,
            locale: Locale?,
            logger: Logger,
            isLinkWithStripe: IsLinkWithStripe,
            fraudDetectionDataRepository: FraudDetectionDataRepository,
            elementsSessionContext: ElementsSessionContext?
        ): FinancialConnectionsConsumerSessionRepository =
            FinancialConnectionsConsumerSessionRepositoryImpl(
                consumersApiService = consumersApiService,
                provideApiRequestOptions = provideApiRequestOptions,
                financialConnectionsConsumersApiService = financialConnectionsConsumersApiService,
                consumerSessionRepository = consumerSessionRepository,
                locale = locale,
                logger = logger,
                fraudDetectionDataRepository = fraudDetectionDataRepository,
                isLinkWithStripe = isLinkWithStripe,
                elementsSessionContext = elementsSessionContext,
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
    private val fraudDetectionDataRepository: FraudDetectionDataRepository,
    private val elementsSessionContext: ElementsSessionContext?,
    isLinkWithStripe: IsLinkWithStripe,
) : FinancialConnectionsConsumerSessionRepository {

    private val mutex = Mutex()

    private val requestSurface: String = if (isLinkWithStripe()) {
        "android_instant_debits"
    } else {
        "android_connections"
    }

    init {
        fraudDetectionDataRepository.refresh()
    }

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
        val incentive = elementsSessionContext?.incentive ?: false
        val paymentIntentId = elementsSessionContext?.paymentIntentId.takeIf { incentive }
        val setupIntentId = elementsSessionContext?.setupIntentId.takeIf { incentive }
        val sessionId = elementsSessionContext?.sessionId.takeIf { incentive }

        consumersApiService.signUp(
            email = email,
            phoneNumber = phoneNumber,
            country = country,
            name = null,
            locale = locale,
            amount = elementsSessionContext?.amount,
            currency = elementsSessionContext?.currency,
            paymentIntentId = paymentIntentId,
            setupIntentId = setupIntentId,
            sessionId = sessionId,
            requestOptions = provideApiRequestOptions(useConsumerPublishableKey = false),
            requestSurface = requestSurface,
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
            requestSurface = requestSurface,
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
            requestSurface = requestSurface,
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
            requestSurface = requestSurface,
            requestOptions = provideApiRequestOptions(useConsumerPublishableKey = false),
        )
    }

    override suspend fun createPaymentDetails(
        bankAccountId: String,
        consumerSessionClientSecret: String,
        billingDetails: BillingDetails?,
    ): ConsumerPaymentDetails {
        return consumersApiService.createPaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            paymentDetailsCreateParams = ConsumerPaymentDetailsCreateParams.BankAccount(
                bankAccountId = bankAccountId,
                billingAddress = billingDetails?.toConsumerBillingAddressParams(),
                billingEmailAddress = billingDetails?.email,
            ),
            requestSurface = requestSurface,
            requestOptions = provideApiRequestOptions(useConsumerPublishableKey = true),
        ).getOrThrow()
    }

    override suspend fun updateIncentiveEligibility(
        paymentDetailId: String,
        paymentIntentId: String?,
        setupIntentId: String?,
    ): ConsumerFinancialIncentive {
        return consumersApiService.updateIncentiveEligibility(
            paymentDetailId = paymentDetailId,
            paymentIntentId = paymentIntentId,
            setupIntentId = setupIntentId,
            requestSurface = requestSurface,
            requestOptions = provideApiRequestOptions(useConsumerPublishableKey = false),
        ).getOrThrow()
    }

    override suspend fun sharePaymentDetails(
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
        expectedPaymentMethodType: String,
        billingPhone: String?,
    ): SharePaymentDetails {
        val fraudDetectionData = fraudDetectionDataRepository.getCached()?.params.orEmpty()
        val expandParams = mapOf("expand" to listOf("payment_method"))

        return consumersApiService.sharePaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            paymentDetailsId = paymentDetailsId,
            expectedPaymentMethodType = expectedPaymentMethodType,
            billingPhone = elementsSessionContext?.billingDetails?.phone?.takeIf { it.isNotBlank() },
            requestSurface = requestSurface,
            requestOptions = provideApiRequestOptions(useConsumerPublishableKey = false),
            extraParams = fraudDetectionData + expandParams,
        ).getOrThrow()
    }

    private suspend fun postConsumerSession(
        email: String,
        clientSecret: String
    ): ConsumerSessionLookup = financialConnectionsConsumersApiService.postConsumerSession(
        email = email,
        clientSecret = clientSecret,
        requestSurface = requestSurface,
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
}
