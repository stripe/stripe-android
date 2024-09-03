package com.stripe.android.utils

import com.stripe.android.paymentsheet.example.test.BuildConfig
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class GooglePayTestRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                if (description.hasGooglePayTestAnnotation()) {
                    if (shouldRunGooglePayTests()) {
                        base.evaluate()
                    }
                } else {
                    base.evaluate()
                }
            }
        }
    }

    private fun Description.hasGooglePayTestAnnotation(): Boolean {
        for (annotation in annotations) {
            if (annotation is GooglePayTest) {
                return true
            }
        }

        return false
    }

    private fun shouldRunGooglePayTests(): Boolean {
        return BuildConfig.SHOULD_RUN_GOOGLE_PAY_TESTS
    }
}
