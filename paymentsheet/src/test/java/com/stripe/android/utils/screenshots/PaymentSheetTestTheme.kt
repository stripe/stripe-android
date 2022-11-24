package com.stripe.android.utils.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.ui.core.PaymentsTheme

@Composable
internal fun PaymentSheetTestTheme(
    config: ComponentTestConfig,
    padding: PaddingValues = PaddingValues(vertical = 16.dp),
    content: @Composable () -> Unit,
) {
    val sheetAppearance = config.paymentSheetAppearance.appearance
    sheetAppearance.parseAppearance()

    PaymentsTheme(
        colors = PaymentsTheme.getColors(isDark = config.systemAppearance == SystemAppearance.Dark),
    ) {
        Surface(color = MaterialTheme.colors.surface) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(padding),
            ) {
                content()
            }
        }
    }
}
