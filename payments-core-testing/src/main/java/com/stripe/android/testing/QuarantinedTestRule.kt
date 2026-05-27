package com.stripe.android.testing

import android.os.Bundle
import android.os.Parcelable
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.parcelize.Parcelize
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

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
        arguments.getParcelableArrayList<Match>(QUARANTINE_ENV_KEY).orEmpty()
    }

    fun match(description: Description): Boolean {
        val className = description.className ?: return false
        val methodName = description.methodName ?: return false
        return quarantinedCases.any { it.className == className && it.testCaseName == methodName }
    }

    private companion object {
        private const val QUARANTINE_ENV_KEY = "bitriseQuarantinedTests"
    }
}

@Parcelize
data class Match(
    val className: String,
    val testCaseName: String,
) : Parcelable
