package com.stripe.android.core.networking

import android.util.Log
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
    private val retryDelaySupplier: RetryDelaySupplier = ExponentialBackoffRetryDelaySupplier(),
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
                "Request failed with code ${stripeResponse.code}. Retrying up to $remainingRetries more time(s)."
            )

            delay(
                retryDelaySupplier.getDelay(
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
            Log.d("TOLUWANI", "RID: ${stripeResponse.requestId?.value}, URL: ${baseUrl}")
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
