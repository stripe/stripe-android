package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.strings.resolve

@Composable
internal fun TapToAddCompletedScreen(
    cardBrand: CardBrand,
    last4: String?,
    label: ResolvableString,
) {
    TapToAddCardLayout {
        TapToAddCard(cardBrand, last4)

        Spacer(Modifier.size(20.dp))

        Title(label)
    }
}

@Composable
private fun Title(label: ResolvableString) {
    Text(
        text = label.resolve(),
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.h4.copy(
            fontWeight = FontWeight.Normal,
        ),
    )
}
