package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SameAsShippingElement(
    override val identifier: IdentifierSpec,
    override val controller: SameAsShippingController
) : SectionSingleFieldElement(identifier) {
    override val shouldRenderOutsideCard: Boolean
        get() = true

    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return controller.formFieldValue.map {
            listOf(
                identifier to it
            )
        }
    }

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        rawValuesMap[identifier]?.let {
            controller.onRawValueChange(it)
        }
    }
}
