package com.stripe.android.link.repositories

import android.app.Application
import com.stripe.android.DefaultFraudDetectionDataRepository
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.confirmation.createPaymentMethodCreateParams
import com.stripe.android.link.utils.toConsumerBillingAddress
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams.Card.Companion.extraConfirmationParams
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionRefresh
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.ConsumerShippingAddresses
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.EmailSource
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.LinkAccountSession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SharePaymentDetails
import com.stripe.android.model.SignUpParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.VerificationType
import com.stripe.android.networking.RequestSurface
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.repository.ConsumersApiService
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Repository that uses [StripeRepository] for Link services.
 */
@SuppressWarnings("TooManyFunctions")
internal class LinkApiRepository @Inject constructor(
    application: Application,
    private val requestSurface: RequestSurface,
    private val apiConfigProvider: () -> ApiConfiguration.State,
    private val stripeRepository: StripeRepository,
    private val consumersApiService: ConsumersApiService,
    @IOContext private val workContext: CoroutineContext,
    private val locale: Locale?,
    private val errorReporter: ErrorReporter,
) : LinkRepository {

    private val fraudDetectionDataRepository: FraudDetectionDataRepository =
        DefaultFraudDetectionDataRepository(application, { apiConfigProvider().publishableKey }, workContext)

    init {
        fraudDetectionDataRepository.refresh()
    }

    override suspend fun lookupConsumer(
        requestOptions: ApiRequest.Options,
        email: String?,
        linkAuthIntentId: String?,
        sessionId: String,
        customerId: String?,
        supportedVerificationTypes: List<String>?,
    ): Result<ConsumerSessionLookup> = withContext(workContext) {
        runCatching {
            requireNotNull(
                consumersApiService.lookupConsumerSession(
                    email = email,
                    linkAuthIntentId = linkAuthIntentId,
                    requestSurface = requestSurface.value,
                    sessionId = sessionId,
                    doNotLogConsumerFunnelEvent = false,
                    requestOptions = requestOptions,
                    customerId = customerId,
                    supportedVerificationTypes = supportedVerificationTypes
                )
            )
        }
    }

    override suspend fun lookupConsumerWithoutBackendLoggingForExposure(
        requestOptions: ApiRequest.Options,
        email: String,
        sessionId: String,
    ): Result<ConsumerSessionLookup> = withContext(workContext) {
        runCatching {
            requireNotNull(
                consumersApiService.lookupConsumerSession(
                    email = email,
                    linkAuthIntentId = null,
                    requestSurface = requestSurface.value,
                    sessionId = sessionId,
                    doNotLogConsumerFunnelEvent = true,
                    supportedVerificationTypes = null,
                    requestOptions = requestOptions,
                    customerId = null
                )
            )
        }
    }

    override suspend fun mobileLookupConsumer(
        requestOptions: ApiRequest.Options,
        email: String?,
        emailSource: EmailSource?,
        linkAuthIntentId: String?,
        verificationToken: String,
        appId: String,
        sessionId: String,
        customerId: String?,
        supportedVerificationTypes: List<String>?,
        linkAuthTokenClientSecret: String?,
    ): Result<ConsumerSessionLookup> = withContext(workContext) {
        runCatching {
            consumersApiService.mobileLookupConsumerSession(
                email = email,
                emailSource = emailSource,
                linkAuthIntentId = linkAuthIntentId,
                requestSurface = requestSurface.value,
                verificationToken = verificationToken,
                appId = appId,
                requestOptions = requestOptions,
                sessionId = sessionId,
                customerId = customerId,
                supportedVerificationTypes = supportedVerificationTypes,
                linkAuthTokenClientSecret = linkAuthTokenClientSecret,
            )
        }
    }

    override suspend fun refreshConsumer(
        requestOptions: ApiRequest.Options,
        appId: String,
        consumerSessionClientSecret: String,
        supportedVerificationTypes: List<String>?,
    ): Result<ConsumerSessionRefresh> = withContext(workContext) {
        runCatching {
            consumersApiService.refreshConsumerSession(
                appId = appId,
                consumerSessionClientSecret = consumerSessionClientSecret,
                supportedVerificationTypes = supportedVerificationTypes,
                requestSurface = requestSurface.value,
                requestOptions = requestOptions,
            )
        }
    }

    override suspend fun consumerSignUp(
        requestOptions: ApiRequest.Options,
        email: String,
        phone: String?,
        country: String?,
        countryInferringMethod: String,
        name: String?,
        consentAction: ConsumerSignUpConsentAction
    ): Result<ConsumerSessionSignup> = withContext(workContext) {
        consumersApiService.signUp(
            SignUpParams(
                email = email,
                phoneNumber = phone,
                country = country,
                countryInferringMethod = countryInferringMethod,
                name = name,
                locale = locale,
                amount = null,
                currency = null,
                incentiveEligibilitySession = null,
                consentAction = consentAction,
                requestSurface = requestSurface.value
            ),
            requestOptions = requestOptions,
        )
    }

    override suspend fun mobileSignUp(
        requestOptions: ApiRequest.Options,
        name: String?,
        email: String,
        phoneNumber: String?,
        country: String?,
        countryInferringMethod: String,
        consentAction: ConsumerSignUpConsentAction,
        amount: Long?,
        currency: String?,
        incentiveEligibilitySession: IncentiveEligibilitySession?,
        verificationToken: String,
        appId: String
    ): Result<ConsumerSessionSignup> = withContext(workContext) {
        consumersApiService.mobileSignUp(
            SignUpParams(
                email = email,
                phoneNumber = phoneNumber,
                country = country,
                countryInferringMethod = countryInferringMethod,
                name = name,
                locale = locale,
                amount = amount,
                currency = currency,
                incentiveEligibilitySession = incentiveEligibilitySession,
                consentAction = consentAction,
                requestSurface = requestSurface.value,
                verificationToken = verificationToken,
                appId = appId
            ),
            requestOptions = requestOptions,
        )
    }

    override suspend fun createCardPaymentDetails(
        requestOptions: ApiRequest.Options,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        userEmail: String,
        stripeIntent: StripeIntent,
        consumerSessionClientSecret: String,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): Result<LinkPaymentDetails.New> = withContext(workContext) {
        consumersApiService.createPaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            paymentDetailsCreateParams = ConsumerPaymentDetailsCreateParams.Card(
                cardPaymentMethodCreateParamsMap = paymentMethodCreateParams.toParamMap(),
                email = userEmail,
            ),
            requestSurface = requestSurface.value,
            requestOptions = requestOptions,
        ).mapCatching {
            val paymentDetails = it.paymentDetails.first()
            val extraParams = extraConfirmationParams(paymentMethodCreateParams.toParamMap())

            val createParams = PaymentMethodCreateParams.createLink(
                paymentDetailsId = paymentDetails.id,
                consumerSessionClientSecret = consumerSessionClientSecret,
                billingDetails = paymentMethodCreateParams.billingDetails,
                extraParams = extraParams,
                allowRedisplay = paymentMethodCreateParams.allowRedisplay,
                clientAttributionMetadata = clientAttributionMetadata,
                originalPaymentMethodCode = paymentMethodCreateParams.typeCode
            )

            LinkPaymentDetails.New(
                paymentDetails = paymentDetails,
                confirmParams = createParams,
                originalParams = paymentMethodCreateParams,
            )
        }.onFailure {
            errorReporter.report(
                ErrorReporter.ExpectedErrorEvent.LINK_CREATE_PAYMENT_DETAILS_FAILURE,
                StripeException.create(it)
            )
        }
    }

    override suspend fun createPaymentDetailsFromPaymentMethod(
        requestOptions: ApiRequest.Options,
        paymentMethod: PaymentMethod,
        userEmail: String,
        stripeIntent: StripeIntent,
        consumerSessionClientSecret: String,
        clientAttributionMetadata: ClientAttributionMetadata,
        customerEphemeralKey: String,
    ): Result<LinkPaymentDetails.Saved> {
        return consumersApiService.createPaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            paymentMethodId = paymentMethod.id,
            requestSurface = requestSurface.value,
            requestOptions = requestOptions,
            customerEphemeralKey = customerEphemeralKey,
        ).mapCatching {
            LinkPaymentDetails.Saved(
                paymentDetails = it.paymentDetails.first(),
                paymentMethod = paymentMethod,
            )
        }
    }

    override suspend fun createBankAccountPaymentDetails(
        requestOptions: ApiRequest.Options,
        bankAccountId: String,
        userEmail: String,
        consumerSessionClientSecret: String,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): Result<ConsumerPaymentDetails.PaymentDetails> = withContext(workContext) {
        consumersApiService.createPaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            paymentDetailsCreateParams = ConsumerPaymentDetailsCreateParams.BankAccount(
                bankAccountId = bankAccountId,
                billingAddress = null,
                billingEmailAddress = userEmail,
                clientAttributionMetadata = clientAttributionMetadata.toParams(),
            ),
            requestSurface = requestSurface.value,
            requestOptions = requestOptions,
        ).mapCatching {
            it.paymentDetails.first()
        }.onFailure {
            errorReporter.report(
                ErrorReporter.ExpectedErrorEvent.LINK_CREATE_PAYMENT_DETAILS_FAILURE,
                StripeException.create(it)
            )
        }
    }

    override suspend fun shareCardPaymentDetails(
        requestOptions: ApiRequest.Options,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        id: String,
        consumerSessionClientSecret: String,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): Result<LinkPaymentDetails.Passthrough> = withContext(workContext) {
        val allowRedisplay = paymentMethodCreateParams.allowRedisplay?.let {
            mapOf(ALLOW_REDISPLAY_PARAM to it.value)
        } ?: emptyMap()
        val billingPhone: Map<String, String> = paymentMethodCreateParams.billingDetails?.phone?.let {
            mapOf("billing_phone" to it)
        } ?: emptyMap()
        val paymentMethodParams = mapOf("expand" to listOf("payment_method"))
        val clientAttributionMetadataParams = clientAttributionMetadata.toParams()

        stripeRepository.sharePaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            id = id,
            extraParams = mapOf(
                "payment_method_options" to extraConfirmationParams(paymentMethodCreateParams.toParamMap()),
            ) + allowRedisplay + billingPhone + paymentMethodParams + clientAttributionMetadataParams,
            requestOptions = requestOptions,
        ).onFailure {
            errorReporter.report(ErrorReporter.ExpectedErrorEvent.LINK_SHARE_CARD_FAILURE, StripeException.create(it))
        }.map { paymentMethod ->
            LinkPaymentDetails.Passthrough(
                paymentDetails = ConsumerPaymentDetails.Passthrough(
                    id = id,
                    last4 = paymentMethodCreateParams.cardLast4().orEmpty(),
                    paymentMethodId = paymentMethod.id,
                    billingEmailAddress = paymentMethod.billingDetails?.email,
                    billingAddress = paymentMethod.billingDetails?.toConsumerBillingAddress(),
                ),
                paymentMethod = paymentMethod,
            )
        }
    }

    override suspend fun sharePaymentDetails(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        expectedPaymentMethodType: String?,
        billingPhone: String?,
        cvc: String?,
        allowRedisplay: String?,
        apiKey: String?,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): Result<SharePaymentDetails> = withContext(workContext) {
        val fraudParams = fraudDetectionDataRepository.getCached()?.params.orEmpty()
        val paymentMethodParams = mapOf("expand" to listOf("payment_method"))
        val optionsParams = cvc?.let {
            mapOf("payment_method_options" to mapOf("card" to mapOf("cvc" to it)))
        } ?: emptyMap()
        val allowRedisplayParams = allowRedisplay?.let {
            mapOf("allow_redisplay" to allowRedisplay)
        } ?: emptyMap()
        val clientAttributionMetadataParams = clientAttributionMetadata.toParams()
        val extraParams = paymentMethodParams +
            fraudParams +
            optionsParams +
            allowRedisplayParams +
            clientAttributionMetadataParams

        val effectiveOptions = buildRequestOptions(requestOptions, apiKey)

        consumersApiService.sharePaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            paymentDetailsId = paymentDetailsId,
            expectedPaymentMethodType = expectedPaymentMethodType,
            requestOptions = effectiveOptions,
            requestSurface = requestSurface.value,
            extraParams = extraParams,
            billingPhone = billingPhone,
        )
    }

    override suspend fun createPaymentMethod(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
        paymentMethod: LinkPaymentMethod,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): Result<PaymentMethod> = withContext(workContext) {
        val params = createPaymentMethodCreateParams(
            selectedPaymentDetails = paymentMethod.details,
            consumerSessionClientSecret = consumerSessionClientSecret,
            cvc = paymentMethod.collectedCvc,
            billingPhone = paymentMethod.billingPhone,
            clientAttributionMetadata = clientAttributionMetadata,
        )
        stripeRepository.createPaymentMethod(
            paymentMethodCreateParams = params,
            options = requestOptions,
        )
    }

    override suspend fun logOut(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
        consumerAccountPublishableKey: String?,
    ): Result<ConsumerSession> = withContext(workContext) {
        stripeRepository.logOut(
            consumerSessionClientSecret = consumerSessionClientSecret,
            consumerAccountPublishableKey = consumerAccountPublishableKey,
            requestOptions = requestOptions,
        )
    }

    override suspend fun startVerification(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
        isResendSmsCode: Boolean
    ): Result<ConsumerSession> {
        return runCatching {
            requireNotNull(
                consumersApiService.startConsumerVerification(
                    consumerSessionClientSecret = consumerSessionClientSecret,
                    locale = locale ?: Locale.US,
                    requestSurface = requestSurface.value,
                    type = VerificationType.SMS,
                    customEmailType = null,
                    connectionsMerchantName = null,
                    requestOptions = requestOptions,
                    isResendSmsCode = isResendSmsCode
                )
            )
        }
    }

    override suspend fun confirmVerification(
        requestOptions: ApiRequest.Options,
        verificationCode: String,
        consumerSessionClientSecret: String,
        consentGranted: Boolean?
    ): Result<ConsumerSession> {
        return runCatching {
            requireNotNull(
                consumersApiService.confirmConsumerVerification(
                    consumerSessionClientSecret = consumerSessionClientSecret,
                    verificationCode = verificationCode,
                    requestSurface = requestSurface.value,
                    type = VerificationType.SMS,
                    consentGranted = consentGranted,
                    requestOptions = requestOptions,
                )
            )
        }
    }

    override suspend fun postConsentUpdate(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
        consentGranted: Boolean,
    ): Result<Unit> = withContext(workContext) {
        consumersApiService.postConsentUpdate(
            consumerSessionClientSecret = consumerSessionClientSecret,
            consentGranted = consentGranted,
            requestSurface = requestSurface.value,
            requestOptions = requestOptions,
        )
    }

    override suspend fun listPaymentDetails(
        requestOptions: ApiRequest.Options,
        paymentMethodTypes: Set<String>,
        consumerSessionClientSecret: String,
    ): Result<ConsumerPaymentDetails> {
        return stripeRepository.listPaymentDetails(
            clientSecret = consumerSessionClientSecret,
            paymentMethodTypes = paymentMethodTypes,
            requestOptions = requestOptions,
        )
    }

    override suspend fun listShippingAddresses(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
    ): Result<ConsumerShippingAddresses> {
        return stripeRepository.listShippingAddresses(
            clientSecret = consumerSessionClientSecret,
            requestOptions = requestOptions,
        )
    }

    override suspend fun deletePaymentDetails(
        requestOptions: ApiRequest.Options,
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
    ): Result<Unit> {
        return stripeRepository.deletePaymentDetails(
            clientSecret = consumerSessionClientSecret,
            paymentDetailsId = paymentDetailsId,
            requestOptions = requestOptions,
        )
    }

    override suspend fun updatePaymentDetails(
        requestOptions: ApiRequest.Options,
        updateParams: ConsumerPaymentDetailsUpdateParams,
        consumerSessionClientSecret: String,
    ): Result<ConsumerPaymentDetails> {
        return stripeRepository.updatePaymentDetails(
            clientSecret = consumerSessionClientSecret,
            paymentDetailsUpdateParams = updateParams,
            requestOptions = requestOptions,
        )
    }

    override suspend fun createLinkAccountSession(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
        intentToken: String?,
        linkMode: LinkMode?,
    ): Result<LinkAccountSession> {
        return consumersApiService.createLinkAccountSession(
            consumerSessionClientSecret = consumerSessionClientSecret,
            intentToken = intentToken,
            linkMode = linkMode,
            requestSurface = requestSurface.value,
            requestOptions = requestOptions,
        )
    }

    override suspend fun updatePhoneNumber(
        requestOptions: ApiRequest.Options,
        consumerSessionClientSecret: String,
        phoneNumber: String,
    ): Result<ConsumerSession> = withContext(workContext) {
        consumersApiService.updatePhoneNumber(
            consumerSessionClientSecret = consumerSessionClientSecret,
            phoneNumber = phoneNumber,
            requestSurface = requestSurface.value,
            requestOptions = requestOptions,
        )
    }

    private fun buildRequestOptions(
        defaultOptions: ApiRequest.Options,
        customApiKey: String?,
    ): ApiRequest.Options {
        return if (customApiKey != null) {
            ApiRequest.Options(
                apiKey = customApiKey,
                stripeAccount = null,
            )
        } else {
            defaultOptions
        }
    }

    private fun ClientAttributionMetadata.toParams(): Map<String, Map<String, Any>> =
        mapOf(CLIENT_ATTRIBUTION_METADATA_PARAM to this.toParamMap())

    private companion object {
        const val ALLOW_REDISPLAY_PARAM = "allow_redisplay"
        const val CLIENT_ATTRIBUTION_METADATA_PARAM = "client_attribution_metadata"
    }
}
