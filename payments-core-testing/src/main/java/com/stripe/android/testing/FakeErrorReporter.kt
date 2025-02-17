package com.stripe.android.testing

import androidx.annotation.RestrictTo
import app.cash.turbine.Event
import app.cash.turbine.Turbine
import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.core.analytics.ErrorReporter

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FakeErrorReporter : ErrorReporter {
    private val calls = Turbine<Call>()

    override fun report(
        errorEvent: ErrorReporter.ErrorEvent,
        stripeException: StripeException?,
        additionalNonPiiParams: Map<String, String>,
    ) {
        calls.add(
            item = Call(
                errorEvent = errorEvent,
                stripeException = stripeException,
                additionalNonPiiParams = additionalNonPiiParams
            )
        )
    }

    suspend fun awaitCall(): Call {
        return calls.awaitItem()
    }

    suspend fun consumeAllEvents(): List<Call> {
        return calls.cancelAndConsumeRemainingEvents()
            .filterIsInstance<Event.Item<Call>>()
            .map { it.value }
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
