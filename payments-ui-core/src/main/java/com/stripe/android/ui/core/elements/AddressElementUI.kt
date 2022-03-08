package com.stripe.android.ui.core.elements

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
internal fun AddressElementUI(
    enabled: Boolean,
    controller: AddressController,
    hiddenIdentifiers: List<IdentifierSpec>?,
    lastTextFieldIdentifier: IdentifierSpec?
) {
    val fields by controller.fieldsFlowable.collectAsState(null)
    fields?.let { fieldList ->
        Column {
            fieldList.forEachIndexed { index, field ->
                SectionFieldElementUI(
                    enabled,
                    field,
                    hiddenIdentifiers = hiddenIdentifiers,
                    lastTextFieldIdentifier = lastTextFieldIdentifier
                )
                if ((hiddenIdentifiers?.contains(field.identifier) == false) &&
                    (index != fieldList.size - 1)
                ) {
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
        }
    }
}
