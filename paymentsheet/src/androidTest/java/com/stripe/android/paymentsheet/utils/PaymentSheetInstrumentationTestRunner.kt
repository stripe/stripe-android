package com.stripe.android.paymentsheet.utils

import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.test.runner.AndroidJUnitRunner
import java.util.Locale

internal class PaymentSheetInstrumentationTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        arguments?.getString(TEST_LOCALE_ARGUMENT)?.let { languageTag ->
            val locale = Locale.forLanguageTag(languageTag)
            Locale.setDefault(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                LocaleList.setDefault(LocaleList(locale))
            }
        }
        super.onCreate(arguments)
    }

    private companion object {
        const val TEST_LOCALE_ARGUMENT = "stripeTestLocale"
    }
}
