package com.stripe.android

import androidx.annotation.VisibleForTesting
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal interface AnalyticsRequestExecutor {
    /**
     * Execute the fire-and-forget request asynchronously.
     */
    fun executeAsync(request: AnalyticsRequest)

    class Default(
        private val logger: Logger = Logger.noop()
    ) : AnalyticsRequestExecutor {
        private val connectionFactory = ConnectionFactory.Default()
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.Default + job)

        /**
         * Make the request and ignore the response
         *
         * @return the response status code. Used for testing purposes.
         */
        @VisibleForTesting
        @Throws(APIConnectionException::class, InvalidRequestException::class)
        internal fun execute(request: AnalyticsRequest): Int {
            connectionFactory.create(request).use {
                try {
                    // required to trigger the request
                    return it.responseCode
                } catch (e: IOException) {
                    throw APIConnectionException.create(e, request.baseUrl)
                }
            }
        }

        override fun executeAsync(request: AnalyticsRequest) {
            scope.launch {
                try {
                    coroutineScope {
                        execute(request)
                    }
                } catch (e: Exception) {
                    logger.error("Exception while making analytics request", e)
                }
            }
        }
    }
}
