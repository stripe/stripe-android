package com.stripe.android.financialconnections.utils

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.TimeZone

class TimeZoneRule(
    private val timeZone: TimeZone = TimeZone.getTimeZone("America/Los_Angeles"),
) : TestWatcher() {

    private val original: TimeZone = TimeZone.getDefault()

    override fun starting(description: Description) {
        super.starting(description)
        TimeZone.setDefault(timeZone)
    }

    override fun finished(description: Description) {
        TimeZone.setDefault(original)
        super.finished(description)
    }
}
