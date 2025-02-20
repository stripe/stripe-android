package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.shouldUseDarkDynamicColor

@Composable
internal fun SelectedBadge(
    modifier: Modifier = Modifier,
) {
    val iconColor = MaterialTheme.colors.primary
    val checkSymbolColor = if (iconColor.shouldUseDarkDynamicColor()) {
        Color.Black
    } else {
        Color.White
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(end = 2.dp)
            .clip(CircleShape)
            .size(24.dp)
            .background(MaterialTheme.colors.primary)
    ) {
        Icon(
            painter = painterResource(R.drawable.stripe_ic_selected_symbol),
            contentDescription = null,
            tint = checkSymbolColor,
            modifier = Modifier.size(12.dp),
        )
    }
}
