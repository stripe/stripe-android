package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.toPaymentElementThemeValues
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.utils.withUiMode

@Composable
internal fun PaymentElementTheme(
    appearance: PaymentSheet.Appearance,
    content: @Composable () -> Unit,
) {
    val isDark = appearance.themeMode.isDarkTheme(isSystemInDarkTheme())
    val themeValues = remember(appearance) {
        appearance.toPaymentElementThemeValues()
    }
    val baseContext = LocalContext.current
    val configuration = LocalConfiguration.current
    val inspectionMode = LocalInspectionMode.current
    val styleContext = remember(baseContext, configuration, isDark, inspectionMode) {
        baseContext.withUiMode(
            configuration = configuration,
            isDark = isDark,
            inspectionMode = inspectionMode,
        )
    }

    CompositionLocalProvider(
        LocalContext provides styleContext,
    ) {
        StripeTheme(
            isDark = isDark,
            colors = if (isDark) themeValues.colorsDark else themeValues.colorsLight,
            shapes = themeValues.shapes,
            typography = themeValues.typography,
            primaryButtonStyle = themeValues.primaryButtonStyle,
            formInsets = themeValues.formInsets,
            sectionSpacing = themeValues.sectionSpacing,
            sectionStyle = StripeThemeDefaults.sectionStyle,
            textFieldInsets = themeValues.textFieldInsets,
            iconStyle = themeValues.iconStyle,
            verticalModeRowPadding = themeValues.verticalModeRowPadding,
            content = content,
        )
    }
}

internal fun PaymentSheet.ThemeMode.isDarkTheme(isSystemDark: Boolean): Boolean {
    return when (this) {
        PaymentSheet.ThemeMode.Automatic -> isSystemDark
        PaymentSheet.ThemeMode.AlwaysLight -> false
        PaymentSheet.ThemeMode.AlwaysDark -> true
    }
}
