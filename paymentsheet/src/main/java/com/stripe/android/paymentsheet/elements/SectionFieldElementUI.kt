package com.stripe.android.paymentsheet.elements

import androidx.compose.runtime.Composable

@Composable
internal fun SectionFieldElementUI(
    enabled: Boolean,
    field: SectionFieldElement,
    hiddenIdentifiers: List<IdentifierSpec>? = null
) {
    if (hiddenIdentifiers?.contains(field.identifier) == false) {
        when (val controller = field.sectionFieldErrorController()) {
            is TextFieldController -> {
                TextField(
                    textFieldController = controller,
                    enabled = enabled
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
                AddressElementUI(
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
