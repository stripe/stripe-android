package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.stripeColors
import com.stripe.android.ui.core.R as PaymentsUiCoreR

@Composable
internal fun NfcCloseButton(
    onPress: () -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val outline = if (isDarkTheme) {
        Modifier.border(width = Dp.Hairline, color = Color.White.copy(alpha = 0.2f), shape = CircleShape)
    } else {
        Modifier.shadow(elevation = 3.dp, shape = CircleShape)
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .then(outline)
            .background(MaterialTheme.colors.surface, CircleShape)
            .clip(CircleShape)
            .clickable { onPress() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(PaymentsUiCoreR.drawable.stripe_ic_rounded_close),
            contentDescription = "Cancel",
            tint = MaterialTheme.stripeColors.appBarIcon,
            modifier = Modifier.size(16.dp)
        )
    }
}
