package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo

/**
 * This is used to define each section in the visual form layout specification
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class FormItemSpec : Parcelable {
    internal fun createSectionElement(
        sectionFieldElement: SectionFieldElement,
        label: Int? = null
    ) =
        SectionElement(
            IdentifierSpec.Generic("${sectionFieldElement.identifier.value}_section"),
            sectionFieldElement,
            SectionController(
                label,
                listOf(sectionFieldElement.sectionFieldErrorController())
            )
        )
}
