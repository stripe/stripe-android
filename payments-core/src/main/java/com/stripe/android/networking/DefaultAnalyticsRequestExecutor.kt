package com.stripe.android.networking

import androidx.annotation.VisibleForTesting
import com.stripe.android.Logger
import com.stripe.android.exception.APIConnectionException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.coroutines.CoroutineContext

internal class DefaultAnalyticsRequestExecutor(
    private val logger: Logger = Logger.noop(),
    private val workContext: CoroutineContext = Dispatchers.IO
) : AnalyticsRequestExecutor {
    private val connectionFactory = ConnectionFactory.Default()

    /**
     * Make the request and ignore the response
     *
     * @return the response status code. Used for testing purposes.
     */
    @VisibleForTesting
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
        logger.info("Event: ${request.params[AnalyticsRequestFactory.FIELD_EVENT]}")

        CoroutineScope(workContext).launch {
            runCatching {
                execute(request)
            }.onFailure {
                logger.error("Exception while making analytics request", it)
            }
        }
    }
}
