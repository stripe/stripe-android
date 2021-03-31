package com.stripe.android.networking

import androidx.annotation.VisibleForTesting
import com.stripe.android.Logger
import com.stripe.android.Stripe
import com.stripe.android.exception.APIConnectionException
import kotlinx.coroutines.delay
import java.io.IOException

internal interface ApiRequestExecutor {
    suspend fun execute(request: ApiRequest): StripeResponse

    suspend fun execute(request: FileUploadRequest): StripeResponse

    /**
     * Used by [StripeApiRepository] to make Stripe API requests
     */
    class Default internal constructor(
        private val connectionFactory: ConnectionFactory = ConnectionFactory.Default(),
        private val retryDelaySupplier: RetryDelaySupplier = RetryDelaySupplier(),
        private val logger: Logger = Logger.noop()
    ) : ApiRequestExecutor {

        private val maxRetries: Int get() = Stripe.maxRetries

        override suspend fun execute(
            request: ApiRequest
        ): StripeResponse = executeInternal(request, maxRetries)

        override suspend fun execute(
            request: FileUploadRequest
        ): StripeResponse = executeInternal(request, maxRetries)

        @VisibleForTesting
        internal suspend fun executeInternal(
            request: StripeRequest,
            remainingRetries: Int
        ): StripeResponse {
            logger.info("Firing request: $request")

            val stripeResponse = makeRequest(request)

            return if (stripeResponse.isRateLimited && remainingRetries > 0) {
                logger.info(
                    "Request was rate-limited with $remainingRetries remaining retries."
                )

                delay(
                    retryDelaySupplier.getDelayMillis(
                        maxRetries,
                        remainingRetries
                    )
                )
                executeInternal(request, remainingRetries - 1)
            } else {
                stripeResponse
            }
        }

        private fun makeRequest(
            request: StripeRequest
        ): StripeResponse {
            return connectionFactory.create(request).use {
                runCatching {
                    val stripeResponse = it.response
                    logger.info(stripeResponse.toString())
                    stripeResponse
                }.getOrElse { error ->
                    logger.error("Exception while making Stripe API request", error)

                    throw when (error) {
                        is IOException -> APIConnectionException.create(error, request.baseUrl)
                        else -> error
                    }
                }
            }
        }
    }
}
