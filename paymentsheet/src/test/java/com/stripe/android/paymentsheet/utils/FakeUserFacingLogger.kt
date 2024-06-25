package com.stripe.android.paymentsheet.utils

import com.stripe.android.core.utils.UserFacingLogger

class FakeUserFacingLogger : UserFacingLogger {

    private val loggedMessages: MutableList<String> = mutableListOf()

    override fun logWarningWithoutPii(message: String) {
        loggedMessages.add(message)
    }

    fun getLoggedMessages(): List<String> {
        return loggedMessages.toList()
    }
}
