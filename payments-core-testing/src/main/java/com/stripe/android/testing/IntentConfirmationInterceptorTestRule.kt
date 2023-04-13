package com.stripe.android.testing

import com.stripe.android.IntentConfirmationInterceptor
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class IntentConfirmationInterceptorTestRule : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        IntentConfirmationInterceptor.createIntentCallback = null
    }

    override fun finished(description: Description) {
        IntentConfirmationInterceptor.createIntentCallback = null
        super.finished(description)
    }
}
