package com.stripe.android.ui.core.elements

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun CardDetailsElementUI(
    enabled: Boolean,
    controller: CardDetailsController,
    hiddenIdentifiers: List<IdentifierSpec>?,
    lastTextFieldIdentifier: IdentifierSpec?
) {
    controller.fields.forEachIndexed { index, field ->
        SectionFieldElementUI(
            enabled,
            field,
            hiddenIdentifiers = hiddenIdentifiers,
            lastTextFieldIdentifier = lastTextFieldIdentifier
        )
        Divider(
            color = CardStyle.cardBorderColor,
            thickness = CardStyle.cardBorderWidth,
            modifier = Modifier.padding(
                horizontal = CardStyle.cardBorderWidth
            )
        )
    }
}
