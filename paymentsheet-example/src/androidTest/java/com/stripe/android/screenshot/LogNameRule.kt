package com.stripe.android.screenshot

import android.util.Log
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

internal class LogNameRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                Log.d("LogNameRule", description.methodName)
                base.evaluate()
            }
        }
    }
}
