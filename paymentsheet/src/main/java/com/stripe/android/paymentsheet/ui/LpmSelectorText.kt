package com.stripe.android.paymentsheet.ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.stripe.android.ui.core.PaymentsTheme

@Composable
internal fun LpmSelectorText(
    text: String,
    textColor: Color,
    modifier: Modifier,
    isEnabled: Boolean
) {
    Text(
        text = text,
        style = PaymentsTheme.typography.caption,
        color = if (isEnabled) textColor else textColor.copy(alpha = 0.6f),
        lineHeight = 1.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
