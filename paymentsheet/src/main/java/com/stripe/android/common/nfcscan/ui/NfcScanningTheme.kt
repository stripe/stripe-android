package com.stripe.android.common.nfcscan.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.stripeThemeIsDark

@Composable
internal fun NfcScanningTheme(
    content: @Composable () -> Unit,
) {
    StripeTheme(
        isDark = MaterialTheme.stripeThemeIsDark,
        typography = NfcScanningThemeDefaults.typography,
    ) {
        content()
    }
}

private object NfcScanningThemeDefaults {
    val typography = StripeThemeDefaults.typography.copy(
        h4 = TextStyle(
            fontSize = 24.sp,
            letterSpacing = 0.sp,
            fontWeight = FontWeight.W700,
        )
    )
}
