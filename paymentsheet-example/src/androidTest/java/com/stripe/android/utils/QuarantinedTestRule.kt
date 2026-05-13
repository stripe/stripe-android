package com.stripe.android.utils

import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONException
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class QuarantinedTestRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val quarantined = matchesQuarantine(description)
                Assume.assumeFalse(
                    buildString {
                        append("Skipping quarantined test ")
                        append(description.className)
                        append("#")
                        append(description.methodName)
                    },
                    quarantined,
                )
                base.evaluate()
            }
        }
    }

    private fun matchesQuarantine(description: Description): Boolean {
        val className = description.className ?: return false
        val methodName = description.methodName ?: return false
        return quarantinedCases.any { it.className == className && it.testCaseName == methodName }
    }

    private data class QuarantineCase(
        val className: String,
        val testCaseName: String,
    )

    companion object {
        private val quarantinedCases: List<QuarantineCase> by lazy { loadQuarantinedCases() }

        private fun loadQuarantinedCases(): List<QuarantineCase> {
            return quarantinedTestsJson()?.let {
                parseCases(it)
            } ?: emptyList()
        }

        private fun parseCases(array: JSONArray): List<QuarantineCase> = buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val className = obj.optString("className").takeIf { it.isNotEmpty() } ?: continue
                val testCaseName = obj.optString("testCaseName").takeIf { it.isNotEmpty() } ?: continue
                add(QuarantineCase(className = className, testCaseName = testCaseName))
            }
        }

        private fun quarantinedTestsJson(): JSONArray? {
            return try {
                InstrumentationRegistry.getArguments()
                    .getString(QUARANTINE_ENV_KEY)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.hexToByteArray()
                    ?.decodeToString()
                    ?.let { JSONArray(it) }
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: JSONException) {
                null
            }
        }

        private const val QUARANTINE_ENV_KEY = "bitriseQuarantinedTests"
    }
}
