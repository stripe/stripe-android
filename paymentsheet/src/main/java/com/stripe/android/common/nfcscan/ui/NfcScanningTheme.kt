package com.stripe.android.common.nfcscan.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.PaymentElementTheme

@Composable
internal fun NfcScanningTheme(
    appearance: PaymentSheet.Appearance,
    content: @Composable () -> Unit,
) {
    PaymentElementTheme(appearance = appearance) {
        MaterialTheme(
            typography = MaterialTheme.typography.copy(
                h4 = MaterialTheme.typography.h4.merge(NfcScanningThemeDefaults.h4),
            ),
            content = content,
        )
    }
}

private object NfcScanningThemeDefaults {
    val h4 = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.3.sp,
    )
}
