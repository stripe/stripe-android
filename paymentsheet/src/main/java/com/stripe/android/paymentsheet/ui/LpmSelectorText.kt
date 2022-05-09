package com.stripe.android.paymentsheet.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.PaymentsTheme

@Composable
internal fun LpmSelectorText(
    @DrawableRes icon: Int? = null,
    text: String,
    textColor: Color,
    modifier: Modifier,
    isEnabled: Boolean
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                modifier = Modifier.padding(horizontal = 4.dp),
                painter = painterResource(it),
                contentDescription = null,
                tint = PaymentsTheme.colors.material.onSurface
            )
        }
        Text(
            text = text,
            style = PaymentsTheme.typography.caption,
            color = if (isEnabled) textColor else textColor.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
