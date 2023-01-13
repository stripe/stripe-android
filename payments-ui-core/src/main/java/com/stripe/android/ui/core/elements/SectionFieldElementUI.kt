package com.stripe.android.ui.core.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.ImeAction
import com.stripe.android.uicore.elements.DropDown
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElementUI
import com.stripe.android.uicore.elements.TextField
import com.stripe.android.uicore.elements.TextFieldController

@Composable
internal fun SectionFieldElementUI(
    enabled: Boolean,
    field: SectionFieldElement,
    modifier: Modifier = Modifier,
    hiddenIdentifiers: Set<IdentifierSpec> = emptySet(),
    lastTextFieldIdentifier: IdentifierSpec?,
    nextFocusDirection: FocusDirection = FocusDirection.Down,
    previousFocusDirection: FocusDirection = FocusDirection.Up
) {
    if (!hiddenIdentifiers.contains(field.identifier)) {
        when (val controller = field.sectionFieldErrorController()) {
            is SameAsShippingController -> {
                SameAsShippingElementUI(
                    controller = controller
                )
            }
            is AddressTextFieldController -> {
                AddressTextFieldUI(controller)
            }
            is TextFieldController -> {
                TextField(
                    textFieldController = controller,
                    enabled = enabled,
                    imeAction = if (lastTextFieldIdentifier == field.identifier) {
                        ImeAction.Done
                    } else {
                        ImeAction.Next
                    },
                    modifier = modifier,
                    nextFocusDirection = nextFocusDirection,
                    previousFocusDirection = previousFocusDirection
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
            else -> {}
        }
    }
}
