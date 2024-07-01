package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    override val allowsUserInteraction: Boolean = fields.any { it.allowsUserInteraction }

    override val mandateText: ResolvableString? = fields.firstNotNullOfOrNull { it.mandateText }

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        combineAsStateFlow(fields.map { it.getFormFieldValueFlow() }) {
            it.toList().flatten()
        }

    override fun getTextFieldIdentifiers(): StateFlow<List<IdentifierSpec>> =
        combineAsStateFlow(
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
