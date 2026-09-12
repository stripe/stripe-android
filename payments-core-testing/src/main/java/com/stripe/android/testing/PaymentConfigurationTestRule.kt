package com.stripe.android.testing

import android.content.Context
import com.stripe.android.PaymentConfiguration
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class PaymentConfigurationTestRule(
    private val context: Context,
    private val publishableKey: String = PUBLISHABLE_KEY,
    private val stripeAccountId: String? = STRIPE_ACCOUNT,
) : TestWatcher() {
    override fun starting(description: Description) {
        PaymentConfiguration.init(context, publishableKey, stripeAccountId)
        super.starting(description)
    }

    override fun finished(description: Description) {
        super.finished(description)
        PaymentConfiguration.clearInstance()
    }

    companion object {
        const val PUBLISHABLE_KEY = "pk_test_123"
        const val STRIPE_ACCOUNT = "acct_123"
    }
}
