package com.stripe.android.link.repositories

import android.app.Application
import com.stripe.android.DefaultFraudDetectionDataRepository
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams.Card.Companion.extraConfirmationParams
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.EmailSource
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SharePaymentDetails
import com.stripe.android.model.SignUpParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.VerificationType
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.repository.ConsumersApiService
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/**
 * Repository that uses [StripeRepository] for Link services.
 */
@SuppressWarnings("TooManyFunctions")
internal class LinkApiRepository @Inject constructor(
    application: Application,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
    private val stripeRepository: StripeRepository,
    private val consumersApiService: ConsumersApiService,
    @IOContext private val workContext: CoroutineContext,
    private val locale: Locale?,
    private val errorReporter: ErrorReporter,
) : LinkRepository {

    private val fraudDetectionDataRepository: FraudDetectionDataRepository =
        DefaultFraudDetectionDataRepository(application, workContext)

    init {
        fraudDetectionDataRepository.refresh()
    }

    override suspend fun lookupConsumer(
        email: String,
        emailSource: EmailSource
        ): Result<ConsumerSessionLookup> = withContext(workContext) {
        runCatching {
            requireNotNull(
                consumersApiService.lookupConsumerSession(
                    email = email,
                    requestSurface = REQUEST_SURFACE,
                    doNotLogConsumerFunnelEvent = false,
                    requestOptions = buildRequestOptions(),
                )
            )
        }
    }

    override suspend fun lookupConsumerWithoutBackendLoggingForExposure(
        email: String
    ): Result<ConsumerSessionLookup> = withContext(workContext) {
        runCatching {
            requireNotNull(
                consumersApiService.lookupConsumerSession(
                    email = email,
                    requestSurface = REQUEST_SURFACE,
                    doNotLogConsumerFunnelEvent = true,
                    requestOptions = buildRequestOptions(),
                )
            )
        }
    }

    override suspend fun mobileLookupConsumer(
        email: String,
        emailSource: EmailSource,
        verificationToken: String,
        appId: String,
        sessionId: String
    ): Result<ConsumerSessionLookup> = withContext(workContext) {
        runCatching {
            consumersApiService.mobileLookupConsumerSession(
                email = email,
                emailSource = emailSource,
                requestSurface = REQUEST_SURFACE,
                verificationToken = verificationToken,
                appId = appId,
                requestOptions = buildRequestOptions(),
                sessionId = sessionId
            )
        }
    }

    override suspend fun consumerSignUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: ConsumerSignUpConsentAction
    ): Result<ConsumerSessionSignup> = withContext(workContext) {
        consumersApiService.signUp(
            SignUpParams(
                email = email,
                phoneNumber = phone,
                country = country,
                name = name,
                locale = locale,
                amount = null,
                currency = null,
                incentiveEligibilitySession = null,
                consentAction = consentAction,
                requestSurface = REQUEST_SURFACE
            ),
            requestOptions = buildRequestOptions(),
        )
    }

    override suspend fun mobileSignUp(
        name: String?,
        email: String,
        phoneNumber: String,
        country: String,
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
                name = name,
                locale = locale,
                amount = amount,
                currency = currency,
                incentiveEligibilitySession = incentiveEligibilitySession,
                consentAction = consentAction,
                requestSurface = REQUEST_SURFACE,
                verificationToken = verificationToken,
                appId = appId
            ),
            requestOptions = buildRequestOptions(),
        )
    }

    override suspend fun createCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        userEmail: String,
        stripeIntent: StripeIntent,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?,
        active: Boolean,
    ): Result<LinkPaymentDetails.New> = withContext(workContext) {
        consumersApiService.createPaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            paymentDetailsCreateParams = ConsumerPaymentDetailsCreateParams.Card(
                cardPaymentMethodCreateParamsMap = paymentMethodCreateParams.toParamMap(),
                email = userEmail,
                active = active,
            ),
            requestSurface = REQUEST_SURFACE,
            requestOptions = buildRequestOptions(consumerPublishableKey),
        ).mapCatching {
            val paymentDetails = it.paymentDetails.first()
            val extraParams = extraConfirmationParams(paymentMethodCreateParams.toParamMap())

            val createParams = PaymentMethodCreateParams.createLink(
                paymentDetailsId = paymentDetails.id,
                consumerSessionClientSecret = consumerSessionClientSecret,
                extraParams = extraParams,
            )

            LinkPaymentDetails.New(
                paymentDetails = paymentDetails,
                paymentMethodCreateParams = createParams,
                originalParams = paymentMethodCreateParams,
            )
        }.onFailure {
            errorReporter.report(ErrorReporter.ExpectedErrorEvent.LINK_CREATE_CARD_FAILURE, StripeException.create(it))
        }
    }

    override suspend fun shareCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        id: String,
        last4: String,
        consumerSessionClientSecret: String,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
    ): Result<LinkPaymentDetails> = withContext(workContext) {
        val allowRedisplayParams = allowRedisplay?.let {
            mapOf(ALLOW_REDISPLAY_PARAM to it.value)
        } ?: emptyMap()

        stripeRepository.sharePaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            id = id,
            extraParams = mapOf(
                "payment_method_options" to extraConfirmationParams(paymentMethodCreateParams.toParamMap()),
            ) + allowRedisplayParams,
            requestOptions = buildRequestOptions(),
        ).onFailure {
            errorReporter.report(ErrorReporter.ExpectedErrorEvent.LINK_SHARE_CARD_FAILURE, StripeException.create(it))
        }.map { passthroughModePaymentMethodId ->
            LinkPaymentDetails.Saved(
                paymentDetails = ConsumerPaymentDetails.Passthrough(
                    id = passthroughModePaymentMethodId,
                    last4 = last4,
                ),
                paymentMethodCreateParams = PaymentMethodCreateParams.createLink(
                    paymentDetailsId = passthroughModePaymentMethodId,
                    consumerSessionClientSecret = consumerSessionClientSecret,
                    extraParams = extraConfirmationParams(paymentMethodCreateParams.toParamMap())
                ),
            )
        }
    }

    override suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        expectedPaymentMethodType: String,
    ): Result<SharePaymentDetails> = withContext(workContext) {
        val fraudParams = fraudDetectionDataRepository.getCached()?.params.orEmpty()
        val paymentMethodParams = mapOf("expand" to listOf("payment_method"))

        consumersApiService.sharePaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            paymentDetailsId = paymentDetailsId,
            expectedPaymentMethodType = expectedPaymentMethodType,
            requestOptions = buildRequestOptions(),
            requestSurface = REQUEST_SURFACE,
            extraParams = paymentMethodParams + fraudParams,
            billingPhone = null,
        )
    }

    override suspend fun logOut(
        consumerSessionClientSecret: String,
        consumerAccountPublishableKey: String?,
    ): Result<ConsumerSession> = withContext(workContext) {
        stripeRepository.logOut(
            consumerSessionClientSecret = consumerSessionClientSecret,
            consumerAccountPublishableKey = consumerAccountPublishableKey,
            requestOptions = buildRequestOptions(consumerAccountPublishableKey),
        )
    }

    override suspend fun startVerification(
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?,
    ): Result<ConsumerSession> {
        return runCatching {
            requireNotNull(
                consumersApiService.startConsumerVerification(
                    consumerSessionClientSecret = consumerSessionClientSecret,
                    locale = locale ?: Locale.US,
                    requestSurface = REQUEST_SURFACE,
                    type = VerificationType.SMS,
                    customEmailType = null,
                    connectionsMerchantName = null,
                    requestOptions = consumerPublishableKey?.let {
                        ApiRequest.Options(it)
                    } ?: ApiRequest.Options(
                        publishableKeyProvider(),
                        stripeAccountIdProvider()
                    )
                )
            )
        }
    }

    override suspend fun confirmVerification(
        verificationCode: String,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<ConsumerSession> {
        return runCatching {
            requireNotNull(
                consumersApiService.confirmConsumerVerification(
                    consumerSessionClientSecret = consumerSessionClientSecret,
                    verificationCode = verificationCode,
                    requestSurface = REQUEST_SURFACE,
                    type = VerificationType.SMS,
                    requestOptions = consumerPublishableKey?.let {
                        ApiRequest.Options(it)
                    } ?: ApiRequest.Options(
                        publishableKeyProvider(),
                        stripeAccountIdProvider()
                    )
                )
            )
        }
    }

    override suspend fun listPaymentDetails(
        paymentMethodTypes: Set<String>,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<ConsumerPaymentDetails> {
        return stripeRepository.listPaymentDetails(
            clientSecret = consumerSessionClientSecret,
            paymentMethodTypes = paymentMethodTypes,
            requestOptions = consumerPublishableKey?.let {
                ApiRequest.Options(it)
            } ?: ApiRequest.Options(
                publishableKeyProvider(),
                stripeAccountIdProvider()
            )
        )
    }

    override suspend fun deletePaymentDetails(
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<Unit> {
        return stripeRepository.deletePaymentDetails(
            clientSecret = consumerSessionClientSecret,
            paymentDetailsId = paymentDetailsId,
            requestOptions = consumerPublishableKey?.let {
                ApiRequest.Options(it)
            } ?: ApiRequest.Options(
                publishableKeyProvider(),
                stripeAccountIdProvider()
            )
        )
    }

    override suspend fun updatePaymentDetails(
        updateParams: ConsumerPaymentDetailsUpdateParams,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<ConsumerPaymentDetails> {
        return stripeRepository.updatePaymentDetails(
            clientSecret = consumerSessionClientSecret,
            paymentDetailsUpdateParams = updateParams,
            requestOptions = consumerPublishableKey?.let {
                ApiRequest.Options(it)
            } ?: ApiRequest.Options(
                publishableKeyProvider(),
                stripeAccountIdProvider()
            )
        )
    }

    private fun buildRequestOptions(
        consumerAccountPublishableKey: String? = null,
    ): ApiRequest.Options {
        return ApiRequest.Options(
            apiKey = consumerAccountPublishableKey ?: publishableKeyProvider(),
            stripeAccount = stripeAccountIdProvider().takeUnless { consumerAccountPublishableKey != null },
        )
    }

    private companion object {
        const val REQUEST_SURFACE = "android_payment_element"
        const val ALLOW_REDISPLAY_PARAM = "allow_redisplay"
    }
}
