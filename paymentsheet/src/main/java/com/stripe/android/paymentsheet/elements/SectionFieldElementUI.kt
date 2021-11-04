package com.stripe.android.paymentsheet.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun SectionFieldElementUI(
    enabled: Boolean,
    field: SectionFieldElement,
    modifier: Modifier = Modifier,
    hiddenIdentifiers: List<IdentifierSpec>? = null
) {
    if (hiddenIdentifiers?.contains(field.identifier) == false) {
        when (val controller = field.sectionFieldErrorController()) {
            is TextFieldController -> {
                TextField(
                    textFieldController = controller,
                    enabled = enabled,
                    modifier = modifier
                )
            }
            is DropdownFieldController -> {
                DropDown(
                    controller.label,
                    controller,
                    enabled
                )
            }
            is AddressController -> {
//                AddressElementUI(
//                    enabled,
//                    controller,
//                    hiddenIdentifiers
//                )
            }
            is RowController -> {
                RowElementUI(
                    enabled,
                    controller,
                    hiddenIdentifiers
                )
            }
            is CardController -> {
                CardDetailsElementUI(
                    enabled,
                    controller,
                    hiddenIdentifiers
                )
            }
        }
    }
}
