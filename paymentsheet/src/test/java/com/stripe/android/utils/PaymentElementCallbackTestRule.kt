package com.stripe.android.utils

import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class PaymentElementCallbackTestRule : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        PaymentElementCallbackReferences.clear()
    }

    override fun finished(description: Description) {
        PaymentElementCallbackReferences.clear()
        super.finished(description)
    }
}
