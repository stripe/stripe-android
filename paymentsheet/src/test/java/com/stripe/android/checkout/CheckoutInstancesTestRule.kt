package com.stripe.android.checkout

import org.junit.rules.TestWatcher
import org.junit.runner.Description

class CheckoutInstancesTestRule : TestWatcher() {
    override fun finished(description: Description) {
        CheckoutInstances.clear()
        super.finished(description)
    }
}
