package com.stripe.android.ui.core.elements

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.paymentsColors
import com.stripe.android.ui.core.paymentsShapes

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
                color = MaterialTheme.paymentsColors.componentDivider,
                thickness = MaterialTheme.paymentsShapes.borderStrokeWidth.dp,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.paymentsShapes.borderStrokeWidth.dp
                )
            )
        }
    }
}
