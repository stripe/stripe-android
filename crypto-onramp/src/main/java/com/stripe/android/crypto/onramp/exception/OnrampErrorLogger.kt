package com.stripe.android.crypto.onramp.exception

import com.stripe.android.core.Logger
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsEvent.ErrorOccurred.Operation
import javax.inject.Inject

internal class OnrampErrorLogger @Inject constructor(
    private val logger: Logger,
) {
    fun log(
        operation: Operation,
        error: Throwable,
    ) {
        logger.error(
            "[Stripe Crypto Onramp] ${operation.value} failed.\n${error.developerLogMessage()}",
            error
        )
    }

    private fun Throwable.developerLogMessage(): String {
        return (this as? StripeCryptoOnrampError)?.developerMessage
            ?: ("Inspect the returned failure error for details. Error type: ${javaClass.simpleName}.\n" +
                "Message: ${this.message}\n" +
                "Cause: ${this.cause?.message}")
    }
}
