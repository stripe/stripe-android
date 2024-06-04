package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AddressElementUI(
    enabled: Boolean,
    controller: AddressController,
    hiddenIdentifiers: Set<IdentifierSpec>,
    lastTextFieldIdentifier: IdentifierSpec?
) {
    val fields by controller.fieldsFlowable.collectAsState(null)

    // The last rendered field is not always the last field in the list.
    // So we need to pre filter so we know when to stop drawing dividers.
    fields?.filterNot { hiddenIdentifiers.contains(it.identifier) }?.let { fieldList ->
        Column {
            fieldList.forEachIndexed { index, field ->
                SectionFieldElementUI(
                    enabled,
                    field,
                    hiddenIdentifiers = hiddenIdentifiers,
                    lastTextFieldIdentifier = lastTextFieldIdentifier
                )
                if (index != fieldList.lastIndex) {
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
    }
}
