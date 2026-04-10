package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun Modifier.paymentMethodIconSize(
    cardArtEnabled: Boolean
): Modifier {
    return width(paymentMethodIconWidth(cardArtEnabled)).height(paymentMethodIconHeight(cardArtEnabled))
}

internal fun paymentMethodIconWidth(cardArtEnabled: Boolean): Dp {
    return if (cardArtEnabled) {
        28.dp
    } else {
        24.dp
    }
}

private fun paymentMethodIconHeight(cardArtEnabled: Boolean): Dp {
    return paymentMethodIconWidth(cardArtEnabled) * 5 / 6f
}
