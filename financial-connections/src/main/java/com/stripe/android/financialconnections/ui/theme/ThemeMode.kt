package com.stripe.android.financialconnections.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.financialconnections.ThemeMode

internal fun ThemeMode.toNightMode(): Int = when (this) {
    ThemeMode.Automatic -> AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
    ThemeMode.AlwaysLight -> AppCompatDelegate.MODE_NIGHT_NO
    ThemeMode.AlwaysDark -> AppCompatDelegate.MODE_NIGHT_YES
}
