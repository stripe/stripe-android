package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.model.Address
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AutocompleteAddressTextFieldController(
    val country: String,
    val googlePlacesApiKey: String
) : InputController, SectionFieldErrorController {
    val address = MutableStateFlow<Address?>(null)

    val config = SimpleTextFieldConfig(
        label = R.string.address_label_address_line1
    )

    override val label: Flow<Int> = MutableStateFlow(config.label)
    override val fieldValue: Flow<String> = address.map { it?.line1 ?: "" }
    override val rawFieldValue: Flow<String?> = address.map { it?.line1 ?: "" }
    override val isComplete: Flow<Boolean> = address.map { it?.line1 != null }
    override val showOptionalLabel: Boolean = false
    override val formFieldValue: Flow<FormFieldEntry> =
        combine(address, isComplete) { address, isComplete ->
            FormFieldEntry(address?.line1, isComplete)
        }

    override fun onRawValueChange(rawValue: String) {

    }

    override val error: Flow<FieldError?> = flowOf(null)
}