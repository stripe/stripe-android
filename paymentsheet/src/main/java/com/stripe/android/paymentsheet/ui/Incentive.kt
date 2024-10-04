package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentMethodIncentive

@Composable
internal fun PaymentMethodIncentive.Content(
    modifier: Modifier = Modifier,
    tinyMode: Boolean = false,
) {
    Box(
        modifier = modifier
            .background(Color(0xFF30B130), RoundedCornerShape(size = 4.dp))
            .padding(
                horizontal = if (tinyMode) 4.dp else 8.dp,
                vertical = if (tinyMode) 0.dp else 4.dp,
            )
    ) {
        Text(
            text = "Get $displayText",
            color = Color.White,
            style = if (tinyMode) {
                MaterialTheme.typography.caption
            } else {
                MaterialTheme.typography.body1
            }
        )
    }
}
