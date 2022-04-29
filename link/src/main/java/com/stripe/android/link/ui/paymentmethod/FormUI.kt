package com.stripe.android.link.ui.paymentmethod

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.shouldUseDarkDynamicColor
import kotlinx.coroutines.flow.Flow

@Composable
internal fun Form(
    formViewModel: FormViewModel,
    enabledFlow: Flow<Boolean>,
) {
    FormUI(
        formViewModel.hiddenIdentifiers,
        enabledFlow,
        formViewModel.elements,
        formViewModel.lastTextFieldIdentifier
    ) {
        Row(
            modifier = Modifier
                .height(100.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val isDark = PaymentsTheme.colors.material.surface.shouldUseDarkDynamicColor()
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = if (isDark) Color.Black else Color.White,
                strokeWidth = 2.dp
            )
        }
    }
}
