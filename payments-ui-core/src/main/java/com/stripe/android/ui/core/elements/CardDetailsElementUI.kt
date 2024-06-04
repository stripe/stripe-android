package com.stripe.android.ui.core.elements

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
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
    lastTextFieldIdentifier: IdentifierSpec?
) {
    controller.fields.forEachIndexed { index, field ->
        // We need to adjust the focus direction, because some devices (Samsung, OnePlus) will
        // navigate to the CVC field if we use `FocusDirection.Down`. We only adjust this for the
        // card number field, because changing this for all fields will allow loops where pressing
        // the backspace in the first field will circle back to the last field.
        val nextFocusDirection = if (field.identifier == IdentifierSpec.CardNumber) {
            FocusDirection.Next
        } else {
            FocusDirection.Down
        }

        SectionFieldElementUI(
            enabled,
            field,
            hiddenIdentifiers = hiddenIdentifiers,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            nextFocusDirection = nextFocusDirection
        )
        if (index != controller.fields.lastIndex) {
            Divider(
                color = MaterialTheme.stripeColors.componentDivider,
                thickness = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.stripeShapes.borderStrokeWidth.dp
                )
            )
        }
    }
}
