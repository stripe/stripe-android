package com.stripe.android.paymentsheet.elements

import androidx.compose.runtime.Composable

@Composable
internal fun SectionFieldElementUI(
    enabled: Boolean,
    field: SectionFieldElement
) {
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
                controller
            )
        }
    }
}
