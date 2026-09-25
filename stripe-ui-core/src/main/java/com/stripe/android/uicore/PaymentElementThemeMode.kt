package com.stripe.android.uicore

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class PaymentElementThemeMode {
    Automatic,
    AlwaysLight,
    AlwaysDark,
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentElementThemeMode.isDarkTheme(isSystemDark: Boolean): Boolean {
    return when (this) {
        PaymentElementThemeMode.Automatic -> isSystemDark
        PaymentElementThemeMode.AlwaysLight -> false
        PaymentElementThemeMode.AlwaysDark -> true
    }
}
