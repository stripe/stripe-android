package com.stripe.android.testing

import android.annotation.SuppressLint
import com.stripe.android.analytics.SessionSavedStateHandler
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@SuppressLint("VisibleForTests")
class SessionTestRule : TestWatcher() {
    override fun starting(description: Description) {
        super.starting(description)
        SessionSavedStateHandler.clear()
    }

    override fun finished(description: Description) {
        SessionSavedStateHandler.clear()
        super.finished(description)
    }
}
