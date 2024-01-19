package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultStripeNetworkClient @JvmOverloads constructor(
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val connectionFactory: ConnectionFactory = ConnectionFactory.Default,
    private val retryDelaySupplier: RetryDelaySupplier = RetryDelaySupplier(),
    private val retryInterceptor: RetryInterceptor = RetryInterceptor(),
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val logger: Logger = Logger.noop()
) : StripeNetworkClient {
    override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
        return executeInternal(
            request = request,
            remainingRetries = maxRetries,
            requester = ::makeRequest
        )
    }

    override suspend fun executeRequestForFile(
        request: StripeRequest,
        outputFile: File
    ): StripeResponse<File> {
        return executeInternal(
            request = request,
            remainingRetries = maxRetries,
            requester = { makeRequestForFile(it, outputFile) }
        )
    }

    @VisibleForTesting
    internal suspend fun <BodyType> executeInternal(
        request: StripeRequest,
        remainingRetries: Int,
        requester: (StripeRequest) -> StripeResponse<BodyType>
    ): StripeResponse<BodyType> = withContext(workContext) {
        val result = runCatching { requester(request) }

        if (retryInterceptor.shouldRetry(request, result) && remainingRetries > 0) {
            result
                .onSuccess { logger.error("Request failed with code ${it.code}. Retrying up to $remainingRetries more time(s).") }
                .onFailure { logger.error("Request failed with exception ${it.message}. Retrying up to $remainingRetries more time(s).") }
            delay(
                retryDelaySupplier.getDelayMillis(
                    DEFAULT_MAX_RETRIES,
                    remainingRetries
                )
            )
            executeInternal(
                request = request,
                remainingRetries = remainingRetries - 1,
                requester = requester
            )
        } else {
            result.getOrThrow()
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
         * Default number of retries if the SDK receives certain range or HTTP codes represented by
         * [StripeRequest.retryResponseCodes].
         */
        private const val DEFAULT_MAX_RETRIES = 3
    }
}
