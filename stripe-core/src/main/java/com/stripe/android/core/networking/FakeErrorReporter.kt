package com.stripe.android.core.networking

class FakeErrorReporter : ErrorReporter {

    private val loggedErrors : MutableList<String> = mutableListOf()
    override fun report(error: ErrorReporter.ErrorEvent) {
        loggedErrors.add(error.eventName)
    }

    fun getLoggedErrors() : List<String> {
        return loggedErrors.toList()
    }
}