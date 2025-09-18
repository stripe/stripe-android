package com.stripe.android.testing

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.view.ContextThemeWrapper
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

    fun contextForLocale(
        base: Context,
    ): Context {
        val config = Configuration(base.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(Locale.getDefault()))
        } else {
            @Suppress("DEPRECATION")
            config.locale = Locale.getDefault()
        }
        val localized = base.createConfigurationContext(config)
        return ContextThemeWrapper(localized, 0)
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
