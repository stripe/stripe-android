package com.stripe.android.ui.core.elements

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.ui.core.PaymentsTheme

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
        if (index != controller.fields.lastIndex) {
            Divider(
                color = PaymentsTheme.colors.colorComponentDivider,
                thickness = PaymentsTheme.shapes.borderStrokeWidth,
                modifier = Modifier.padding(
                    horizontal = PaymentsTheme.shapes.borderStrokeWidth
                )
            )
        }
    }
}
