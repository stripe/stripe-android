package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.strings.resolve

@Composable
internal fun ColumnScope.TapToAddCompletedScreen(
    cardBrand: CardBrand,
    last4: String?,
    label: ResolvableString,
) {
    TapToAddCardLayout(
        cardBrand = cardBrand,
        last4 = last4,
        title = label.resolve(),
    ) {}
}
