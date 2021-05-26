package com.stripe.android.networking

import androidx.annotation.VisibleForTesting
import com.stripe.android.Logger
import com.stripe.android.exception.APIConnectionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.CoroutineContext

/**
 * Used by [StripeApiRepository] to make Stripe API requests
 */
internal class DefaultApiRequestExecutor @JvmOverloads internal constructor(
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val connectionFactory: ConnectionFactory = ConnectionFactory.Default(),
    private val retryDelaySupplier: RetryDelaySupplier = RetryDelaySupplier(),
    private val logger: Logger = Logger.noop()
) : ApiRequestExecutor {

    override suspend fun execute(
        request: ApiRequest
    ): StripeResponse = executeInternal(request, MAX_RETRIES)

    override suspend fun execute(
        request: FileUploadRequest
    ): StripeResponse = executeInternal(request, MAX_RETRIES)

    @VisibleForTesting
    internal suspend fun executeInternal(
        request: StripeRequest,
        remainingRetries: Int
    ): StripeResponse = withContext(workContext) {
        logger.info("Firing request: $request")

        val stripeResponse = makeRequest(request)

        if (stripeResponse.isRateLimited && remainingRetries > 0) {
            logger.info(
                "Request was rate-limited with $remainingRetries remaining retries."
            )

            delay(
                retryDelaySupplier.getDelayMillis(
                    MAX_RETRIES,
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

    private companion object {
        /**
         * If the SDK receives a "Too Many Requests" (429) status code from Stripe,
         * it will automatically retry the request using exponential backoff.
         *
         * The default value is 3.
         *
         * See https://stripe.com/docs/rate-limits for more information.
         */
        private const val MAX_RETRIES = 3
    }
}
