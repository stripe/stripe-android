package com.stripe.android.financialconnections.ui.theme

import com.stripe.android.financialconnections.ThemeMode

internal fun ThemeMode.isDarkTheme(isSystemDark: Boolean): Boolean = when (this) {
    ThemeMode.Automatic -> isSystemDark
    ThemeMode.AlwaysLight -> false
    ThemeMode.AlwaysDark -> true
}
