package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.stripe.android.model.CardBrand

@Composable
internal fun ColumnScope.TapToAddDelayScreen(
    cardBrand: CardBrand,
    last4: String?,
) {
    TapToAddCardLayout(
        cardBrand = cardBrand,
        last4 = last4,
        title = null,
    ) {}
}
