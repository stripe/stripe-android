package com.stripe.android.testing

import android.content.Context
import com.stripe.android.PaymentConfiguration
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class PaymentConfigurationTestRule(
    private val context: Context,
    private val publishableKey: String = "pk_123"
) : TestWatcher() {
    override fun starting(description: Description) {
        PaymentConfiguration.init(context, publishableKey)
        super.starting(description)
    }

    override fun finished(description: Description) {
        super.finished(description)
        PaymentConfiguration.clearInstance()
    }
}
