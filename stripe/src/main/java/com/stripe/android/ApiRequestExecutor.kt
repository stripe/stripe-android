package com.stripe.android

import com.stripe.android.exception.APIConnectionException
import java.io.IOException

internal interface ApiRequestExecutor {
    fun execute(request: ApiRequest): StripeResponse

    fun execute(request: FileUploadRequest): StripeResponse

    /**
     * Used by [StripeApiRepository] to make Stripe API requests
     */
    class Default internal constructor(
        private val logger: Logger = Logger.noop()
    ) : ApiRequestExecutor {
        private val connectionFactory = ConnectionFactory.Default()

        override fun execute(request: ApiRequest): StripeResponse {
            return executeInternal(request)
        }

        override fun execute(request: FileUploadRequest): StripeResponse {
            return executeInternal(request)
        }

        private fun executeInternal(request: StripeRequest): StripeResponse {
            logger.info(request.toString())

            connectionFactory.create(request).use {
                try {
                    val stripeResponse = it.response
                    logger.info(stripeResponse.toString())
                    return stripeResponse
                } catch (e: IOException) {
                    logger.error("Exception while making Stripe API request", e)
                    throw APIConnectionException.create(e, request.baseUrl)
                }
            }
        }
    }
}
