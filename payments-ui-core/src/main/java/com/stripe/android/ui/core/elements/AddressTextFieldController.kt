package com.stripe.android.ui.core.elements

import com.stripe.android.model.Address
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

internal class AddressTextFieldController(
    var country: String?,
    val googlePlacesApiKey: String?,
    val config: TextFieldConfig,
    val onClick: () -> Unit
) : SimpleTextFieldController(config), InputController, SectionFieldErrorController {
    val address = MutableStateFlow<Address?>(null)

    val shouldUseAutocomplete = googlePlacesApiKey != null //&& DefaultIsPlacesAvailable().invoke()

    override val formFieldValue: Flow<FormFieldEntry> =
        combine(address, isComplete) { address, isComplete ->
            FormFieldEntry(address?.line1, isComplete)
        }

    override fun onRawValueChange(rawValue: String) {
        super.onRawValueChange(rawValue)
        country = rawValue
    }
}
