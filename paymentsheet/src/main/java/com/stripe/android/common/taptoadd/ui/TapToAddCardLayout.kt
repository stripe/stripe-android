package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.CardBrand

@Composable
internal fun ColumnScope.TapToAddCardLayout(
    cardBrand: CardBrand,
    last4: String?,
    title: String,
    content: @Composable () -> Unit,
) {
    Spacer(Modifier.size(LocalTapToAddMaxContentHeight.current * SCREEN_POSITION_MULTIPLIER))

    TapToAddCard(
        cardBrand = cardBrand,
        last4 = last4,
    )

    Spacer(Modifier.size(35.dp))

    Text(
        text = title,
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.h4,
    )

    Spacer(Modifier.size(35.dp))

    Spacer(Modifier.weight(1f))

    content()
}

private const val SCREEN_POSITION_MULTIPLIER = 0.03f
