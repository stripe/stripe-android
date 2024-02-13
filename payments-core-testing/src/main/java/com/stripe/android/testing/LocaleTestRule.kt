package com.stripe.android.testing

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Locale

class LocaleTestRule(
    private val locale: Locale,
) : TestWatcher() {

    private val original: Locale = Locale.getDefault()

    override fun starting(description: Description) {
        super.starting(description)
        Locale.setDefault(locale)
    }

    override fun finished(description: Description) {
        Locale.setDefault(original)
        super.finished(description)
    }
}
