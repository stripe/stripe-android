package com.stripe.android.paymentsheet.utils

import android.content.Context
import android.content.SharedPreferences
import com.stripe.android.paymentsheet.DefaultPrefsRepository

class PrefsTestStore(
    context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(DefaultPrefsRepository.PREF_FILE, Context.MODE_PRIVATE)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
