package com.stripe.android.ui.core.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.paymentsColors
import com.stripe.android.ui.core.paymentsShapes

@Composable
internal fun AddressElementUI(
    enabled: Boolean,
    controller: AddressController,
    hiddenIdentifiers: List<IdentifierSpec>?,
    lastTextFieldIdentifier: IdentifierSpec?
) {
    val fields by controller.fieldsFlowable.collectAsState(null)

    // The last rendered field is not always the last field in the list.
    // So we need to pre filter so we know when to stop drawing dividers.
    fields?.filter {
        (hiddenIdentifiers?.contains(it.identifier) == false)
    }?.let { fieldList ->
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
                        color = MaterialTheme.paymentsColors.componentDivider,
                        thickness = MaterialTheme.paymentsShapes.borderStrokeWidth.dp,
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.paymentsShapes.borderStrokeWidth.dp
                        )
                    )
                }
            }
        }
    }
}
