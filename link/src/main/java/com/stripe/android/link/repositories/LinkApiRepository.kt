package com.stripe.android.link.repositories

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
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
        authSessionCookie: String?
    ): Result<ConsumerSession> = withContext(workContext) {
        runCatching {
            stripeRepository.startConsumerVerification(
                consumerSessionClientSecret,
                locale ?: Locale.US,
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
                } ?: Result.failure(InternalError("Error starting consumer verification"))
            },
            onFailure = {
                logger.error("Error starting consumer verification", it)
                Result.failure(it)
            }
        )
    }

    override suspend fun confirmVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        authSessionCookie: String?
    ): Result<ConsumerSession> = withContext(workContext) {
        runCatching {
            stripeRepository.confirmConsumerVerification(
                consumerSessionClientSecret,
                verificationCode,
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
        authSessionCookie: String?
    ): Result<ConsumerSession> = withContext(workContext) {
        runCatching {
            stripeRepository.logoutConsumer(
                consumerSessionClientSecret,
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
                } ?: Result.failure(InternalError("Error logging out"))
            },
            onFailure = {
                logger.error("Error logging out", it)
                Result.failure(it)
            }
        )
    }

    override suspend fun listPaymentDetails(
        consumerSessionClientSecret: String
    ): Result<ConsumerPaymentDetails> = withContext(workContext) {
        runCatching {
            stripeRepository.listPaymentDetails(
                consumerSessionClientSecret,
                setOf("card"),
                ApiRequest.Options(
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
        paymentDetails: ConsumerPaymentDetailsCreateParams,
        consumerSessionClientSecret: String
    ): Result<ConsumerPaymentDetails> = withContext(workContext) {
        runCatching {
            stripeRepository.createPaymentDetails(
                consumerSessionClientSecret,
                paymentDetails,
                ApiRequest.Options(
                    publishableKeyProvider(),
                    stripeAccountIdProvider()
                )
            )
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
}
