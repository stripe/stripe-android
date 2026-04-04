package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultAnalyticsRequestExecutor(
    private val stripeNetworkClient: StripeNetworkClient,
    private val workContext: CoroutineContext,
    private val logger: Logger
) : AnalyticsRequestExecutor {

    constructor() : this(
        Logger.noop(),
        defaultAnalyticsRequestExecutorWorkContext()
    )

    constructor(
        logger: Logger,
        workContext: CoroutineContext
    ) : this(
        createDefaultAnalyticsRequestExecutorNetworkClient(
            workContext = workContext,
            logger = logger,
        ),
        workContext,
        logger
    )

    /**
     * Make the request and ignore the response.
     */
    override fun executeAsync(request: AnalyticsRequest) {
        if (!AnalyticsRequestExecutor.ENABLED) return

        logger.info("Event: ${request.params[FIELD_EVENT]}")

        CoroutineScope(workContext).launch {
            runCatching {
                stripeNetworkClient.executeRequest(request)
            }.onFailure {
                logger.error("Exception while making analytics request", it)
            }
        }
    }

    internal companion object {
        const val FIELD_EVENT = "event"
    }
}
