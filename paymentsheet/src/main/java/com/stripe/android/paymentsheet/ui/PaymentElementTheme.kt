package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.toPaymentElementThemeMode
import com.stripe.android.paymentsheet.toPaymentElementThemeValues
import com.stripe.android.uicore.isDarkTheme
import com.stripe.android.uicore.PaymentElementTheme as UiCorePaymentElementTheme

@Composable
internal fun PaymentElementTheme(
    appearance: PaymentSheet.Appearance,
    content: @Composable () -> Unit,
) {
    val themeValues = remember(appearance) {
        appearance.toPaymentElementThemeValues()
    }

    UiCorePaymentElementTheme(
        values = themeValues,
        content = content,
    )
}

internal fun PaymentSheet.ThemeMode.isDarkTheme(isSystemDark: Boolean): Boolean {
    return toPaymentElementThemeMode().isDarkTheme(isSystemDark)
}
