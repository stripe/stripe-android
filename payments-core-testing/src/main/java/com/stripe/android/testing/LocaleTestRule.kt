package com.stripe.android.testing

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Locale

class LocaleTestRule(
    private val locale: Locale? = null,
) : TestWatcher() {

    private val original: Locale = Locale.getDefault()

    fun setTemporarily(locale: Locale) {
        Locale.setDefault(locale)
    }

    override fun starting(description: Description) {
        super.starting(description)
        if (locale != null) {
            Locale.setDefault(locale)
        }
    }

    override fun finished(description: Description) {
        Locale.setDefault(original)
        super.finished(description)
    }
}
