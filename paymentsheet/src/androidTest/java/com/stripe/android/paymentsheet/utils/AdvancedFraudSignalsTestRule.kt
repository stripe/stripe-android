package com.stripe.android.paymentsheet.utils

import com.stripe.android.Stripe
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class AdvancedFraudSignalsTestRule : TestWatcher() {
    override fun starting(description: Description?) {
        super.starting(description)
        Stripe.advancedFraudSignalsEnabled = false
    }

    override fun finished(description: Description?) {
        Stripe.advancedFraudSignalsEnabled = true
        super.finished(description)
    }
}
