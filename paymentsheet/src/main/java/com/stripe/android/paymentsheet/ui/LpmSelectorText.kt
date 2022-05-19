package com.stripe.android.paymentsheet.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
                tint = MaterialTheme.colors.onSurface
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = if (isEnabled) textColor else textColor.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
