package com.stripe.android.networking

import androidx.annotation.VisibleForTesting
import com.stripe.android.Logger
import com.stripe.android.exception.APIConnectionException
import java.io.IOException

internal interface ApiRequestExecutor {
    fun execute(request: ApiRequest): StripeResponse

    fun execute(request: FileUploadRequest): StripeResponse

    /**
     * Used by [StripeApiRepository] to make Stripe API requests
     */
    class Default internal constructor(
        private val connectionFactory: ConnectionFactory = ConnectionFactory.Default(),
        private val logger: Logger = Logger.noop()
    ) : ApiRequestExecutor {

        override fun execute(request: ApiRequest): StripeResponse {
            return executeInternal(request)
        }

        override fun execute(request: FileUploadRequest): StripeResponse {
            return executeInternal(request)
        }

        @VisibleForTesting
        internal fun executeInternal(request: StripeRequest): StripeResponse {
            logger.info(request.toString())

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
