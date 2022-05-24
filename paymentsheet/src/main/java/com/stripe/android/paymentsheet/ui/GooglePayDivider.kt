package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.paymentsColors
import com.stripe.android.ui.core.paymentsShapes
import com.stripe.android.ui.core.shouldUseDarkDynamicColor

@Composable
internal fun GooglePayDividerUi(
    text: String = stringResource(R.string.stripe_paymentsheet_or_pay_with_card)
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        GooglePayDividerLine()
        Text(
            text = text,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.paymentsColors.subtitle,
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(horizontal = 8.dp)
        )
    }
}

@Composable
internal fun GooglePayDividerLine() {
    val color = if (MaterialTheme.colors.surface.shouldUseDarkDynamicColor()) {
        Color.Black.copy(alpha = .20f)
    } else {
        Color.White.copy(alpha = .20f)
    }
    Box(
        Modifier
            .background(color)
            .height(MaterialTheme.paymentsShapes.borderStrokeWidth.dp)
            .fillMaxWidth()
    )
}
