package com.stripe.android.utils.screenshots

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.ui.core.PaymentsTheme

@Composable
internal fun PaymentSheetTestTheme(
    config: ComponentTestConfig,
    content: @Composable () -> Unit,
) {
    val sheetAppearance = config.paymentSheetAppearance.appearance
    sheetAppearance.parseAppearance()

    PaymentsTheme(
        colors = PaymentsTheme.getColors(isDark = config.systemAppearance == SystemAppearance.Dark),
    ) {
        Surface(color = MaterialTheme.colors.surface) {
            content()
        }
    }
}
