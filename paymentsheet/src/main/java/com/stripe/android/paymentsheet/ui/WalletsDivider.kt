package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.shouldUseDarkDynamicColor
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

@Composable
internal fun WalletsDivider(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        WalletsDividerLine(
            modifier = Modifier.weight(1f),
        )

        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.stripeColors.subtitle,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        WalletsDividerLine(
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WalletsDividerLine(
    modifier: Modifier = Modifier,
) {
    val color = if (MaterialTheme.colors.surface.shouldUseDarkDynamicColor()) {
        Color.Black.copy(alpha = .20f)
    } else {
        Color.White.copy(alpha = .20f)
    }
    Box(
        modifier
            .background(color)
            .height(MaterialTheme.stripeShapes.borderStrokeWidth.dp)
            .fillMaxWidth()
    )
}
