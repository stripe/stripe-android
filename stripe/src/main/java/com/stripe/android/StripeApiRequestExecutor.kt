package com.stripe.android

import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException
import java.io.IOException
import java.net.UnknownHostException

/**
 * Used by [StripeApiRepository] to make HTTP requests
 */
internal class StripeApiRequestExecutor internal constructor(
    private val logger: Logger = Logger.noop()
) : ApiRequestExecutor {
    private val connectionFactory: ConnectionFactory = ConnectionFactory()

    /**
     * Make the request and return the response as a [StripeResponse]
     */
    @Throws(APIConnectionException::class, InvalidRequestException::class, UnknownHostException::class)
    override fun execute(request: ApiRequest): StripeResponse {
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
