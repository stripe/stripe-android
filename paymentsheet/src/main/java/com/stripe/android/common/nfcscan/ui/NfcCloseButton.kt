package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import com.stripe.android.uicore.stripeThemeIsDark
import com.stripe.android.ui.core.R as PaymentsUiCoreR

@Composable
internal fun NfcCloseButton(
    onPress: () -> Unit,
) {
    val color = if (MaterialTheme.stripeThemeIsDark) {
        Color.White
    } else {
        LIGHT_MODE_COLOR
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f), CircleShape)
            .clickable { onPress() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(PaymentsUiCoreR.drawable.stripe_ic_rounded_close),
            contentDescription = "Cancel",
            tint = color,
            modifier = Modifier.size(14.dp)
        )
    }
}

private val LIGHT_MODE_COLOR = Color(0xFF30313D)
