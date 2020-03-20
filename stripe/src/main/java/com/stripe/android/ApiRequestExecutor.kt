package com.stripe.android

import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException
import java.io.IOException
import java.net.UnknownHostException

internal interface ApiRequestExecutor {
    @Throws(APIConnectionException::class, InvalidRequestException::class, UnknownHostException::class)
    fun execute(request: ApiRequest): StripeResponse

    @Throws(APIConnectionException::class, InvalidRequestException::class, UnknownHostException::class)
    fun execute(request: FileUploadRequest): StripeResponse

    /**
     * Used by [StripeApiRepository] to make Stripe API requests
     */
    class Default internal constructor(
        private val logger: Logger = Logger.noop()
    ) : ApiRequestExecutor {
        private val connectionFactory = ConnectionFactory.Default()

        @Throws(APIConnectionException::class, InvalidRequestException::class, UnknownHostException::class)
        override fun execute(request: ApiRequest): StripeResponse {
            return executeInternal(request)
        }

        @Throws(APIConnectionException::class, InvalidRequestException::class, UnknownHostException::class)
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
