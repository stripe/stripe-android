package com.stripe.android.testing

import com.stripe.android.core.utils.FeatureFlag
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class FeatureFlagTestRule(
    private val featureFlag: FeatureFlag,
    private val isEnabled: Boolean,
) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    featureFlag.setEnabled(isEnabled)
                    base.evaluate()
                } finally {
                    featureFlag.reset()
                }
            }
        }
    }

    fun setEnabled(isEnabled: Boolean?) {
        if (isEnabled != null) {
            featureFlag.setEnabled(isEnabled)
            return
        }
        featureFlag.reset()
    }
}
