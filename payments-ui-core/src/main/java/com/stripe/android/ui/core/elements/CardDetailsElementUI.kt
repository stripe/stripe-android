package com.stripe.android.ui.core.elements

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun CardDetailsElementUI(
    enabled: Boolean,
    controller: CardDetailsController,
    hiddenIdentifiers: List<IdentifierSpec>?
) {
    controller.fields.forEachIndexed { index, field ->
        SectionFieldElementUI(enabled, field, hiddenIdentifiers = hiddenIdentifiers)
        val cardStyle = CardStyle(isSystemInDarkTheme())
        Divider(
            color = cardStyle.cardBorderColor,
            thickness = cardStyle.cardBorderWidth,
            modifier = Modifier.padding(
                horizontal = cardStyle.cardBorderWidth
            )
        )
    }
}
