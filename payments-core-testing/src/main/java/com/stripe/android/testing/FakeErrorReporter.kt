package com.stripe.android.testing

import androidx.annotation.RestrictTo
import com.stripe.android.payments.core.analytics.ErrorReporter

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FakeErrorReporter : ErrorReporter {

    private val loggedErrors: MutableList<String> = mutableListOf()

    override fun report(errorEvent: ErrorReporter.ErrorEvent, analyticsValue: String?, statusCode: Int?) {
        loggedErrors.add(errorEvent.eventName)
    }

    fun getLoggedErrors(): List<String> {
        return loggedErrors.toList()
    }

    fun clear() {
        loggedErrors.clear()
    }
}
