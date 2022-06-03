package com.stripe.android.ui.core.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction

@Composable
internal fun SectionFieldElementUI(
    enabled: Boolean,
    field: SectionFieldElement,
    modifier: Modifier = Modifier,
    hiddenIdentifiers: List<IdentifierSpec>? = null,
    lastTextFieldIdentifier: IdentifierSpec?,
) {
    if (hiddenIdentifiers?.contains(field.identifier) == false) {
        when (val controller = field.sectionFieldErrorController()) {
            is TextFieldController -> {
                TextField(
                    textFieldController = controller,
                    enabled = enabled,
                    modifier = modifier,
                    imeAction = if (lastTextFieldIdentifier == field.identifier) {
                        ImeAction.Done
                    } else {
                        ImeAction.Next
                    }
                )
            }
            is DropdownFieldController -> {
                DropDown(
                    controller,
                    enabled
                )
            }
            is AddressController -> {
                AddressElementUI(
                    enabled,
                    controller,
                    hiddenIdentifiers,
                    lastTextFieldIdentifier
                )
            }
            is RowController -> {
                RowElementUI(
                    enabled,
                    controller,
                    hiddenIdentifiers,
                    lastTextFieldIdentifier
                )
            }
            is CardDetailsController -> {
                CardDetailsElementUI(
                    enabled,
                    controller,
                    hiddenIdentifiers,
                    lastTextFieldIdentifier
                )
            }
            is PhoneNumberController -> {
                PhoneNumberElementUI(
                    enabled,
                    controller
                )
            }
            is SaveForFutureUseController -> {}
        }
    }
}
