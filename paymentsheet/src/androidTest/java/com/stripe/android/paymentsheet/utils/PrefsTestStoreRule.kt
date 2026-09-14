package com.stripe.android.paymentsheet.utils

import androidx.test.core.app.ApplicationProvider
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

internal class PrefsTestStoreRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val prefsTestStore = PrefsTestStore(ApplicationProvider.getApplicationContext())
                prefsTestStore.clear()

                try {
                    base.evaluate()
                } finally {
                    prefsTestStore.clear()
                }
            }
        }
    }
}
