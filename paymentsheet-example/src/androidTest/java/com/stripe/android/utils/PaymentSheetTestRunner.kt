package com.stripe.android.utils

import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.runner.AndroidJUnitRunner
import java.util.Locale

internal class PaymentSheetTestRunner : AndroidJUnitRunner() {
    private var testLocaleLanguageTag: String? = null

    override fun onCreate(arguments: Bundle?) {
        testLocaleLanguageTag = arguments?.getString(TEST_LOCALE_ARGUMENT)
        super.onCreate(arguments)
        configureLeakCanaryForManagedDevices()
    }

    override fun onStart() {
        testLocaleLanguageTag?.let { languageTag ->
            val locale = Locale.forLanguageTag(languageTag)
            Locale.setDefault(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                LocaleList.setDefault(LocaleList(locale))
            }
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageTag)
            )
        }
        super.onStart()
    }

    private companion object {
        const val TEST_LOCALE_ARGUMENT = "stripeTestLocale"
    }
}
