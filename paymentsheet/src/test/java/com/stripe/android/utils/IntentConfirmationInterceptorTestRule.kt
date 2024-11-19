package com.stripe.android.utils

import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
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
