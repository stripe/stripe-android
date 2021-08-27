package com.stripe.android.paymentsheet.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.forms.AddressElementUI

@Composable
internal fun SectionFieldElementUI(
    enabled: Boolean,
    field: SectionFieldElement,
    modifier: Modifier = Modifier
) {
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
            AddressElementUI(
                enabled,
                controller
            )
        }
        is RowController -> {
            RowElementUI(
                enabled,
                controller
            )
        }
    }
}
