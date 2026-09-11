package com.stripe.android.paymentsheet

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import app.cash.burst.Burst
import java.util.regex.Pattern

@Suppress("unused")
internal class BurstAndroidJUnitRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        arguments?.let(::rebuildWithAllBurstTests)
        super.onCreate(arguments)
    }

    fun rebuildWithAllBurstTests(arguments: Bundle) = with(arguments) {
        if (containsKey(TESTS_REGEX_ARGUMENT)) return@with

        val classArgument = getString(CLASS_ARGUMENT) ?: return@with
        val testsRegex = findTestsRegex(classArgument) ?: return@with

        remove(CLASS_ARGUMENT)
        putString(TESTS_REGEX_ARGUMENT, testsRegex)
    }

    private fun findTestsRegex(classArgument: String): String? {
        val selections = classArgument.split(',').map { selector ->
            findTestSelection(selector) ?: return null
        }

        if (selections.none(TestSelection::isBurst)) return null

        return selections.joinToString(prefix = "(?:", postfix = ")", separator = "|") {
            it.regex
        }
    }

    private fun findTestSelection(selector: String): TestSelection? {
        val methodSeparatorIndex = selector.indexOf('#')
        val className = selector.substringBefore('#')
        val isBurst = className.isBurstClass() ?: return null

        val regex = if (methodSeparatorIndex == -1) {
            if (isBurst) {
                "^${Pattern.quote(className)}(?:_|#)"
            } else {
                "^${Pattern.quote(className)}#"
            }
        } else {
            val methodName = selector.substring(methodSeparatorIndex + 1)
            if (methodName.isEmpty() || '#' in methodName) return null

            if (isBurst) {
                "^${Pattern.quote(className)}(?:_[^#]+)?#${Pattern.quote(methodName)}(?:_|$)"
            } else {
                "^${Pattern.quote(className)}#${Pattern.quote(methodName)}$"
            }
        }

        return TestSelection(regex = regex, isBurst = isBurst)
    }

    private fun String.isBurstClass(): Boolean? {
        return runCatching {
            Class
                .forName(this, false, this@BurstAndroidJUnitRunner.javaClass.classLoader)
                .isAnnotationPresent(Burst::class.java)
        }.getOrNull()
    }

    private data class TestSelection(
        val regex: String,
        val isBurst: Boolean,
    )

    private companion object {
        const val CLASS_ARGUMENT = "class"
        const val TESTS_REGEX_ARGUMENT = "tests_regex"
    }
}
