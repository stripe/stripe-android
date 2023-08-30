package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
data class BlikSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Blik
) : FormItemSpec() {
    fun transform(): SectionElement {
        return createSectionElement(
            sectionFieldElement = BlikElement(),
        )
    }
}
