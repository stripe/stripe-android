package com.stripe.android.ui.core.elements

import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionFieldElementUI
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

@Composable
internal fun CardDetailsElementUI(
    enabled: Boolean,
    controller: CardDetailsController,
    hiddenIdentifiers: Set<IdentifierSpec>,
    lastTextFieldIdentifier: IdentifierSpec?,
    modifier: Modifier = Modifier,
) {
    controller.fields.forEachIndexed { index, field ->
        SectionFieldElementUI(
            enabled,
            field,
            modifier = modifier,
            hiddenIdentifiers = hiddenIdentifiers,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
        )
        if (index != controller.fields.lastIndex) {
            Divider(
                color = MaterialTheme.stripeColors.componentDivider,
                thickness = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
            )
        }
    }
}
