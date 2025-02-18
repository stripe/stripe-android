package com.stripe.android.testing

import androidx.annotation.RestrictTo
import app.cash.turbine.Turbine
import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.core.analytics.ErrorReporter

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FakeErrorReporter : ErrorReporter {
    private val calls = Turbine<Call>()
    private val loggedErrors: MutableList<String> = mutableListOf()

    override fun report(
        errorEvent: ErrorReporter.ErrorEvent,
        stripeException: StripeException?,
        additionalNonPiiParams: Map<String, String>,
    ) {
        loggedErrors.add(errorEvent.eventName)
        calls.add(
            item = Call(
                errorEvent = errorEvent,
                stripeException = stripeException,
                additionalNonPiiParams = additionalNonPiiParams
            )
        )
    }

    fun getLoggedErrors(): List<String> {
        return loggedErrors.toList()
    }

    fun clear() {
        loggedErrors.clear()
    }

    suspend fun awaitCall(): Call {
        return calls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    data class Call(
        val errorEvent: ErrorReporter.ErrorEvent,
        val stripeException: StripeException?,
        val additionalNonPiiParams: Map<String, String>
    )
}
