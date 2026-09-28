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
import java.net.URI
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
        return parseResponse(connectionFactory.create(request), request)
    }

    private fun makeRequestForFile(
        request: StripeRequest,
        outputFile: File
    ): StripeResponse<File> {
        return parseResponse(connectionFactory.createForFile(request, outputFile), request)
    }

    private fun <BodyType> parseResponse(
        connection: StripeConnection<BodyType>,
        request: StripeRequest
    ): StripeResponse<BodyType> =
        runCatching {
            val stripeResponse = connection.response
            logger.info(stripeResponse.toLogString(request))
            stripeResponse
        }.getOrElse { error ->
            logger.error("Exception while making Stripe API request", error)

            throw when (error) {
                is IOException -> APIConnectionException.create(error, request.url)
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

private fun <BodyType> StripeResponse<BodyType>.toLogString(
    request: StripeRequest
): String {
    return "Request: ${request.method.code} ${request.sanitizedLogTarget()}, " +
        "${StripeResponse.HEADER_REQUEST_ID}: ${requestId?.value ?: "absent"}, " +
        "Status Code: $code"
}

private fun StripeRequest.sanitizedLogTarget(): String {
    val uri = runCatching {
        URI(url)
    }.getOrNull()

    val host = uri?.host
    val path = uri?.rawPath
        ?.takeIf { it.isNotBlank() }
        ?.redactSensitivePathSegments()
        ?: "/"

    return if (host == null || host == "api.stripe.com") {
        path
    } else {
        "$host$path"
    }
}

private fun String.redactSensitivePathSegments(): String {
    return split("/")
        .joinToString("/") { segment ->
            if (segment.shouldRedactPathSegment()) {
                REDACTED_PATH_SEGMENT
            } else {
                segment
            }
        }
}

private fun String.shouldRedactPathSegment(): Boolean {
    return STRIPE_ID_PATH_SEGMENT_REGEX.matches(this) ||
        HIGH_ENTROPY_PATH_SEGMENT_REGEX.matches(this)
}

private const val REDACTED_PATH_SEGMENT = "[redacted]"

private val STRIPE_ID_PATH_SEGMENT_REGEX = Regex(
    pattern = "^(acct|ba|btok|card|cn|cs|ct|ctoken|cus|cvsess|ephkey|es|fca|fcsess|" +
        "in|link|pi|pm|req|seti|si|src|tok)_[A-Za-z0-9_]+$"
)

private val HIGH_ENTROPY_PATH_SEGMENT_REGEX = Regex(
    pattern = "^(?=.*\\d)[A-Za-z0-9_-]{16,}$"
)
