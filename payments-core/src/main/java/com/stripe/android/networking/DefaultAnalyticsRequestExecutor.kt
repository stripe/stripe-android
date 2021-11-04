package com.stripe.android.networking

import androidx.annotation.VisibleForTesting
import com.stripe.android.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.networking.ConnectionFactory
import com.stripe.android.payments.core.injection.IOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultAnalyticsRequestExecutor @Inject constructor(
    private val logger: Logger,
    @IOContext private val workContext: CoroutineContext
) : AnalyticsRequestExecutor {
    internal constructor() : this(
        Logger.noop(),
        Dispatchers.IO
    )

    private val connectionFactory = ConnectionFactory.Default

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
                throw APIConnectionException.create(e, request.url)
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
