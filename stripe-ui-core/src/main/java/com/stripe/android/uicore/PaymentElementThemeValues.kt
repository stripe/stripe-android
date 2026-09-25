package com.stripe.android.uicore

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentElementThemeValues(
    val colorsLight: StripeColors,
    val colorsDark: StripeColors,
    val themeMode: PaymentElementThemeMode,
    val shapes: StripeShapes,
    val typography: StripeTypography,
    val primaryButtonStyle: PrimaryButtonStyle,
    val formInsets: FormInsets,
    val sectionSpacing: Float?,
    val textFieldInsets: FormInsets,
    val iconStyle: IconStyle,
    val verticalModeRowPadding: Float,
)
