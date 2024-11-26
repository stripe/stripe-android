package com.stripe.android.link.repositories

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams.Card.Companion.extraConfirmationParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.PaymentMethodCreateParams
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
internal class LinkApiRepository @Inject constructor(
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
    private val stripeRepository: StripeRepository,
    private val consumersApiService: ConsumersApiService,
    @IOContext private val workContext: CoroutineContext,
    private val locale: Locale?,
    private val errorReporter: ErrorReporter,
) : LinkRepository {

    override suspend fun lookupConsumer(
        email: String,
    ): Result<ConsumerSessionLookup> = withContext(workContext) {
        runCatching {
            requireNotNull(
                consumersApiService.lookupConsumerSession(
                    email = email,
                    requestSurface = REQUEST_SURFACE,
                    requestOptions = buildRequestOptions(),
                )
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
            email = email,
            phoneNumber = phone,
            country = country,
            name = name,
            locale = locale,
            amount = null,
            currency = null,
            paymentIntentId = null,
            setupIntentId = null,
            consentAction = consentAction,
            requestOptions = buildRequestOptions(),
            requestSurface = REQUEST_SURFACE,
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
    ): Result<LinkPaymentDetails> = withContext(workContext) {
        stripeRepository.sharePaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            id = id,
            extraParams = mapOf(
                "payment_method_options" to extraConfirmationParams(paymentMethodCreateParams.toParamMap()),
            ),
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
    }
}
