package com.stripe.android.testing

import org.junit.rules.TestWatcher
import org.junit.runner.Description

internal class CleanupTestRule<T>(
    private val cleanup: T.() -> Unit,
) : TestWatcher() {
    private val tracked = mutableListOf<T>()

    fun track(objectToCleanup: T): T = objectToCleanup.also { tracked.add(it) }

    override fun finished(description: Description) {
        tracked.forEach { it.cleanup() }
        super.finished(description)
    }
}
