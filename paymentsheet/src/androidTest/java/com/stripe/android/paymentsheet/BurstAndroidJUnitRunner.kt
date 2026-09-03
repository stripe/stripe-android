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
        val testsRegex = findBurstTests(classArgument) ?: return@with

        remove(CLASS_ARGUMENT)
        putString(TESTS_REGEX_ARGUMENT, testsRegex)
    }

    private fun findBurstTests(classArgument: String): String? {
        if (',' in classArgument) {
            return null
        }

        val methodSeparatorIndex = classArgument.indexOf('#')
        val className = classArgument.substringBefore('#')
        if (!className.isBurstClass()) return null

        return if (methodSeparatorIndex == -1) {
            "^${Pattern.quote(className)}(?:_|#)"
        } else {
            val methodName = classArgument.substring(methodSeparatorIndex + 1)
            if (methodName.isEmpty() || '#' in methodName) return null

            "^${Pattern.quote(className)}(?:_[^#]+)?#${Pattern.quote(methodName)}(?:_|$)"
        }
    }

    private fun String.isBurstClass(): Boolean {
        return runCatching {
            Class
                .forName(this, false, this@BurstAndroidJUnitRunner.javaClass.classLoader)
                .isAnnotationPresent(Burst::class.java)
        }.getOrDefault(false)
    }

    private companion object {
        const val CLASS_ARGUMENT = "class"
        const val TESTS_REGEX_ARGUMENT = "tests_regex"
    }
}
