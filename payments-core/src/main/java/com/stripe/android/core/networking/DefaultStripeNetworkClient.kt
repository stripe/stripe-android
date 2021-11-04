package com.stripe.android.core.networking

import androidx.annotation.VisibleForTesting
import com.stripe.android.Logger
import com.stripe.android.core.exception.APIConnectionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.CoroutineContext

internal class DefaultStripeNetworkClient @JvmOverloads constructor(
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val connectionFactory: ConnectionFactory = ConnectionFactory.Default,
    private val retryDelaySupplier: RetryDelaySupplier = RetryDelaySupplier(),
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val logger: Logger = Logger.noop()
) : StripeNetworkClient {
    override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
        return executeInternal(maxRetries, request.retryResponseCodes) {
            makeRequest(request)
        }
    }

    override suspend fun executeRequestForFile(
        request: StripeRequest,
        outputFile: File
    ): StripeResponse<File> {
        return executeInternal(maxRetries, request.retryResponseCodes) {
            makeRequestForFile(request, outputFile)
        }
    }

    @VisibleForTesting
    internal suspend fun <BodyType> executeInternal(
        remainingRetries: Int,
        retryResponseCodes: Iterable<Int>,
        requester: () -> StripeResponse<BodyType>
    ): StripeResponse<BodyType> = withContext(workContext) {

        val stripeResponse = requester()

        if (retryResponseCodes.contains(stripeResponse.code) && remainingRetries > 0) {
            logger.info(
                "Request was rate-limited with $remainingRetries remaining retries."
            )

            delay(
                retryDelaySupplier.getDelayMillis(
                    DEFAULT_MAX_RETRIES,
                    remainingRetries
                )
            )
            executeInternal(remainingRetries - 1, retryResponseCodes, requester)
        } else {
            stripeResponse
        }
    }

    private fun makeRequest(
        request: StripeRequest
    ): StripeResponse<String> {
        return parseResponse(connectionFactory.create(request), request.url)
    }

    private fun makeRequestForFile(
        request: StripeRequest,
        outputFile: File
    ): StripeResponse<File> {
        return parseResponse(connectionFactory.createForFile(request, outputFile), request.url)
    }

    private fun <BodyType> parseResponse(
        connection: StripeConnection<BodyType>,
        baseUrl: String?
    ): StripeResponse<BodyType> =
        runCatching {
            val stripeResponse = connection.response
            logger.info(stripeResponse.toString())
            stripeResponse
        }.getOrElse { error ->
            logger.error("Exception while making Stripe API request", error)

            throw when (error) {
                is IOException -> APIConnectionException.create(error, baseUrl)
                else -> error
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
        private const val DEFAULT_MAX_RETRIES = 3
    }
}
