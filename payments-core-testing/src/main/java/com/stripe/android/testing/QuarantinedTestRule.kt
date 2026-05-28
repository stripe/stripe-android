package com.stripe.android.testing

import android.os.Bundle
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONException
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

class QuarantinedTestRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val quarantined = matcher.match(description)

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

    private companion object {
        val matcher by lazy {
            QuarantinedTestMatcher(InstrumentationRegistry.getArguments())
        }
    }
}

internal class QuarantinedTestMatcher(
    private val arguments: Bundle,
) {
    private val quarantinedCases: List<Match> by lazy {
        decode()
    }

    fun match(description: Description): Boolean {
        val className = description.className ?: return false
        val methodName = description.methodName ?: return false
        return quarantinedCases.any { it.className == className && it.testCaseName == methodName }
    }

    private fun decode(): List<Match> {
        val encoded = arguments.getString(QUARANTINE_ENV_KEY)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return emptyList()

        return try {
            val compressed = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP)
            val rawJson = GZIPInputStream(ByteArrayInputStream(compressed))
                .bufferedReader()
                .use { it.readText() }
            val listOfTestsJson = JSONArray(rawJson)

            parseQuarantineCases(listOfTestsJson)
        } catch (_: IllegalArgumentException) {
            return emptyList()
        } catch (_: JSONException) {
            return emptyList()
        } catch (_: java.io.IOException) {
            return emptyList()
        }
    }

    private fun parseQuarantineCases(array: JSONArray): List<Match> = buildList {
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i)
            val className = obj?.optString("className")?.takeIf { it.isNotEmpty() }
            val testCaseName = obj?.optString("testCaseName")?.takeIf { it.isNotEmpty() }

            if (className == null || testCaseName == null) {
                continue
            }

            add(Match(className = className, testCaseName = testCaseName))
        }
    }

    data class Match(
        val className: String,
        val testCaseName: String,
    )

    private companion object {
        private const val QUARANTINE_ENV_KEY = "bitriseQuarantinedTests"
    }
}
