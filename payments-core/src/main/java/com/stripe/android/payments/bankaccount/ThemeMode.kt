package com.stripe.android.payments.bankaccount

import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.ThemeMode as FinancialConnectionsThemeMode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class ThemeMode {
    Automatic,
    AlwaysLight,
    AlwaysDark,
}

internal fun ThemeMode.toFinancialConnectionsThemeMode(): FinancialConnectionsThemeMode = when (this) {
    ThemeMode.Automatic -> FinancialConnectionsThemeMode.Automatic
    ThemeMode.AlwaysLight -> FinancialConnectionsThemeMode.AlwaysLight
    ThemeMode.AlwaysDark -> FinancialConnectionsThemeMode.AlwaysDark
}
