package com.stripe.android.link.repositories

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.confirmation.ConfirmStripeIntentParamsFactory
import com.stripe.android.link.ui.paymentmethod.SupportedPaymentMethod
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Repository that uses [StripeRepository] for Link services.
 */
@Singleton
internal class LinkApiRepository @Inject constructor(
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
    private val stripeRepository: StripeRepository,
    private val logger: Logger,
    @IOContext private val workContext: CoroutineContext,
    private val locale: Locale?
) : LinkRepository {

    override suspend fun lookupConsumer(
        email: String?,
        authSessionCookie: String?
    ): Result<ConsumerSessionLookup> = withContext(workContext) {
        runCatching {
            stripeRepository.lookupConsumerSession(
                email,
                authSessionCookie,
                ApiRequest.Options(
                    publishableKeyProvider(),
                    stripeAccountIdProvider()
                )
            )
        }.fold(
            onSuccess = {
                it?.let {
                    Result.success(it)
                } ?: Result.failure(InternalError("Error looking up consumer"))
            },
            onFailure = {
                logger.error("Error looking up consumer", it)
                Result.failure(it)
            }
        )
    }

    override suspend fun consumerSignUp(
        email: String,
        phone: String,
        country: String,
        authSessionCookie: String?
    ): Result<ConsumerSession> = withContext(workContext) {
        runCatching {
            stripeRepository.consumerSignUp(
                email,
                phone,
                country,
                locale,
                authSessionCookie,
                ApiRequest.Options(
                    publishableKeyProvider(),
                    stripeAccountIdProvider()
                )
            )
        }.fold(
            onSuccess = {
                it?.let {
                    Result.success(it)
                } ?: Result.failure(InternalError("Error signing up consumer"))
            },
            onFailure = {
                logger.error("Error signing up consumer", it)
                Result.failure(it)
            }
        )
    }

    override suspend fun startVerification(
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?,
        authSessionCookie: String?
    ): Result<ConsumerSession> = withContext(workContext) {
        runCatching {
            stripeRepository.startConsumerVerification(
                consumerSessionClientSecret,
                locale ?: Locale.US,
                authSessionCookie,
                consumerPublishableKey?.let {
                    ApiRequest.Options(it)
                } ?: ApiRequest.Options(
                    publishableKeyProvider(),
                    stripeAccountIdProvider()
                )
            )
        }.fold(
            onSuccess = {
                it?.let {
                    Result.success(it)
                } ?: Result.failure(InternalError("Error starting consumer verification"))
            },
            onFailure = {
                logger.error("Error starting consumer verification", it)
                Result.failure(it)
            }
        )
    }

    override suspend fun confirmVerification(
        verificationCode: String,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?,
        authSessionCookie: String?
    ): Result<ConsumerSession> = withContext(workContext) {
        runCatching {
            stripeRepository.confirmConsumerVerification(
                consumerSessionClientSecret,
                verificationCode,
                authSessionCookie,
                consumerPublishableKey?.let {
                    ApiRequest.Options(it)
                } ?: ApiRequest.Options(
                    publishableKeyProvider(),
                    stripeAccountIdProvider()
                )
            )
        }.fold(
            onSuccess = {
                it?.let {
                    Result.success(it)
                } ?: Result.failure(InternalError("Error confirming consumer verification"))
            },
            onFailure = {
                logger.error("Error confirming consumer verification", it)
                Result.failure(it)
            }
        )
    }

    override suspend fun logout(
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?,
        authSessionCookie: String?
    ): Result<ConsumerSession> = withContext(workContext) {
        runCatching {
            stripeRepository.logoutConsumer(
                consumerSessionClientSecret,
                authSessionCookie,
                consumerPublishableKey?.let {
                    ApiRequest.Options(it)
                } ?: ApiRequest.Options(
                    publishableKeyProvider(),
                    stripeAccountIdProvider()
                )
            )
        }.fold(
            onSuccess = {
                it?.let {
                    Result.success(it)
                } ?: Result.failure(InternalError("Error logging out"))
            },
            onFailure = {
                logger.error("Error logging out", it)
                Result.failure(it)
            }
        )
    }

    override suspend fun listPaymentDetails(
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<ConsumerPaymentDetails> = withContext(workContext) {
        runCatching {
            stripeRepository.listPaymentDetails(
                consumerSessionClientSecret,
                setOf("card"),
                consumerPublishableKey?.let {
                    ApiRequest.Options(it)
                } ?: ApiRequest.Options(
                    publishableKeyProvider(),
                    stripeAccountIdProvider()
                )
            )
        }.fold(
            onSuccess = {
                it?.let {
                    Result.success(it)
                } ?: Result.failure(InternalError("Error fetching consumer payment details"))
            },
            onFailure = {
                logger.error("Error fetching consumer payment details", it)
                Result.failure(it)
            }
        )
    }

    override suspend fun createPaymentDetails(
        paymentMethod: SupportedPaymentMethod,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        userEmail: String,
        stripeIntent: StripeIntent,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<LinkPaymentDetails.New> = withContext(workContext) {
        runCatching {
            stripeRepository.createPaymentDetails(
                consumerSessionClientSecret,
                paymentMethod.createParams(paymentMethodCreateParams, userEmail),
                consumerPublishableKey?.let {
                    ApiRequest.Options(it)
                } ?: ApiRequest.Options(
                    publishableKeyProvider(),
                    stripeAccountIdProvider()
                )
            )?.paymentDetails?.first()?.let {
                LinkPaymentDetails.New(
                    it,
                    ConfirmStripeIntentParamsFactory.createFactory(stripeIntent)
                        .createPaymentMethodCreateParams(
                            consumerSessionClientSecret,
                            it,
                            paymentMethod.extraConfirmationParams(paymentMethodCreateParams)
                        ),
                    paymentMethodCreateParams
                )
            }
        }.fold(
            onSuccess = {
                it?.let {
                    Result.success(it)
                } ?: Result.failure(InternalError("Error creating consumer payment method"))
            },
            onFailure = {
                logger.error("Error creating consumer payment method", it)
                Result.failure(it)
            }
        )
    }

    override suspend fun updatePaymentDetails(
        updateParams: ConsumerPaymentDetailsUpdateParams,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<ConsumerPaymentDetails> = withContext(workContext) {
        runCatching {
            stripeRepository.updatePaymentDetails(
                consumerSessionClientSecret,
                updateParams,
                consumerPublishableKey?.let {
                    ApiRequest.Options(it)
                } ?: ApiRequest.Options(
                    publishableKeyProvider(),
                    stripeAccountIdProvider()
                )
            )
        }.fold(
            onSuccess = {
                it?.let {
                    Result.success(it)
                } ?: Result.failure(InternalError("Error updating consumer payment method"))
            },
            onFailure = {
                logger.error("Error updating consumer payment method", it)
                Result.failure(it)
            }
        )
    }

    override suspend fun deletePaymentDetails(
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<Unit> = withContext(workContext) {
        runCatching {
            stripeRepository.deletePaymentDetails(
                consumerSessionClientSecret,
                paymentDetailsId,
                consumerPublishableKey?.let {
                    ApiRequest.Options(it)
                } ?: ApiRequest.Options(
                    publishableKeyProvider(),
                    stripeAccountIdProvider()
                )
            )
        }.fold(
            onSuccess = {
                Result.success(Unit)
            },
            onFailure = {
                logger.error("Error deleting consumer payment method", it)
                Result.failure(it)
            }
        )
    }
}
