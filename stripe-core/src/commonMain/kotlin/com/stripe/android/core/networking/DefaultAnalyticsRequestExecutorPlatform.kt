package com.stripe.android.core.networking

import com.stripe.android.core.Logger
import kotlin.coroutines.CoroutineContext

internal expect fun defaultAnalyticsRequestExecutorWorkContext(): CoroutineContext

internal expect fun createDefaultAnalyticsRequestExecutorNetworkClient(
    workContext: CoroutineContext,
    logger: Logger,
): StripeNetworkClient
