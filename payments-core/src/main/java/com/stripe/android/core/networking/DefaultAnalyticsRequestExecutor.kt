package com.stripe.android.core.networking

import com.stripe.android.Logger
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.injection.IOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultAnalyticsRequestExecutor(
    private val stripeNetworkClient: StripeNetworkClient,
    @IOContext private val workContext: CoroutineContext,
    private val logger: Logger
) : AnalyticsRequestExecutor {

    internal constructor() : this(
        Logger.noop(),
        Dispatchers.IO
    )

    @Inject
    internal constructor(
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
        logger.info("Event: ${request.params[PaymentAnalyticsRequestFactory.FIELD_EVENT]}")

        CoroutineScope(workContext).launch {
            runCatching {
                stripeNetworkClient.executeRequest(request)
            }.onFailure {
                logger.error("Exception while making analytics request", it)
            }
        }
    }
}
