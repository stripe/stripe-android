package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultAnalyticsRequestExecutor(
    private val stripeNetworkClient: StripeNetworkClient,
    @IOContext private val workContext: CoroutineContext,
    private val logger: Logger
) : AnalyticsRequestExecutor {

    constructor() : this(
        Logger.noop(),
        Dispatchers.IO
    )

    @Inject
    constructor(
        logger: Logger,
        @IOContext workContext: CoroutineContext
    ) : this(
        DefaultStripeNetworkClient(
            workContext = workContext,
            logger = logger
        ),
        workContext,
        logger
    )

    /**
     * Make the request and ignore the response.
     */
    override fun executeAsync(request: AnalyticsRequest) {
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
