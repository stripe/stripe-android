package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SectionElement(
    override val identifier: IdentifierSpec,
    val fields: List<SectionFieldElement>,
    override val controller: SectionController
) : FormElement {
    constructor(
        identifier: IdentifierSpec,
        field: SectionFieldElement,
        controller: SectionController
    ) : this(identifier, listOf(field), controller)

    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        combine(fields.map { it.getFormFieldValueFlow() }) {
            it.toList().flatten()
        }

    override fun getTextFieldIdentifiers(): Flow<List<IdentifierSpec>> =
        combine(
            fields
                .map {
                    it.getTextFieldIdentifiers()
                }
        ) {
            it.toList().flatten()
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        fun wrap(
            sectionFieldElement: SectionFieldElement,
            label: Int? = null
        ): SectionElement {
            return wrap(
                sectionFieldElements = listOf(sectionFieldElement),
                label = label
            )
        }

        fun wrap(
            sectionFieldElements: List<SectionFieldElement>,
            label: Int? = null
        ): SectionElement {
            val errorControllers = sectionFieldElements.map {
                it.sectionFieldErrorController()
            }
            return SectionElement(
                IdentifierSpec.Generic("${sectionFieldElements.first().identifier.v1}_section"),
                sectionFieldElements,
                SectionController(
                    label,
                    errorControllers
                )
            )
        }
    }
}
