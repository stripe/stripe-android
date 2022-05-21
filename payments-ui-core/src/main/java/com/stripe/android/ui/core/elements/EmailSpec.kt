package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class EmailSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Email
) : FormItemSpec(), RequiredItemSpec {
    fun transform(initialValues: Map<IdentifierSpec, String?>) = createSectionElement(
        EmailElement(
            this.api_path,
            initialValue = initialValues[IdentifierSpec.Email]
        )
    )
}
