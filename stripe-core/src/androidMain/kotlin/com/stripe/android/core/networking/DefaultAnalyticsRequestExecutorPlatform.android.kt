package com.stripe.android.core.networking

import com.stripe.android.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

internal actual fun defaultAnalyticsRequestExecutorWorkContext(): CoroutineContext {
    return Dispatchers.IO
}

internal actual fun createDefaultAnalyticsRequestExecutorNetworkClient(
    workContext: CoroutineContext,
    logger: Logger,
): StripeNetworkClient {
    return DefaultStripeNetworkClient(
        workContext = workContext,
        logger = logger,
    )
}
