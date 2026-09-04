package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SameAsShippingElement(
    override val identifier: FormFieldId,
    override val controller: SameAsShippingController
) : FormElement {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    fun setRawValue(rawValuesMap: Map<FormFieldId, String?>) {
        rawValuesMap[identifier]?.let {
            controller.onRawValueChange(it)
        }
    }

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<FormFieldId, FormFieldEntry>>> {
        return controller.formFieldValue.mapAsStateFlow { formFieldEntry ->
            listOf(identifier to formFieldEntry)
        }
    }
}
