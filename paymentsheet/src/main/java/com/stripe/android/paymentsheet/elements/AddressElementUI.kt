package com.stripe.android.paymentsheet.elements

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
    hiddenIdentifiers: List<IdentifierSpec>?
) {
    val fields by controller.fieldsFlowable.collectAsState(null)
    if (fields != null) {
        Column {
            fields!!.forEachIndexed { index, field ->
                SectionFieldElementUI(enabled, field, hiddenIdentifiers = hiddenIdentifiers)
                if ((hiddenIdentifiers?.contains(field.identifier) == false) &&
                    (index != fields!!.size - 1)
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
