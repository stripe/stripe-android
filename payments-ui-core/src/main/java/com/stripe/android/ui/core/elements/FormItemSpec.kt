package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * This is used to define each section in the visual form layout specification
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
sealed class FormItemSpec : Parcelable {
    abstract val api_path: IdentifierSpec?

    internal fun createSectionElement(
        sectionFieldElement: SectionFieldElement,
        label: Int? = null
    ) =
        SectionElement(
            IdentifierSpec.Generic("${sectionFieldElement.identifier.v1}_section"),
            sectionFieldElement,
            SectionController(
                label,
                listOf(sectionFieldElement.sectionFieldErrorController())
            )
        )
}
