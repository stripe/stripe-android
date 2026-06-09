package com.stripe.android.crypto.onramp.exception

import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsEvent.ErrorOccurred.Operation
import javax.inject.Inject

internal class OnrampErrorLogger @Inject constructor(
    private val userFacingLogger: UserFacingLogger,
) {
    fun log(
        operation: Operation,
        error: Throwable,
    ) {
        userFacingLogger.logWarningWithoutPii(
            "[Stripe Crypto Onramp] ${operation.value} failed.\n${error.developerLogMessage()}"
        )
    }

    private fun Throwable.developerLogMessage(): String {
        return (this as? StripeCryptoOnrampError)?.developerMessage
            ?: "Inspect the returned failure error for details. Error type: ${javaClass.simpleName}."
    }
}
