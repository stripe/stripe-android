package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class EmailSpec(
    override val apiPath: IdentifierSpec = DEFAULT_API_PATH
) : FormItemSpec() {
    fun transform(initialValues: Map<IdentifierSpec, String?>) = createSectionElement(
        EmailElement(
            this.apiPath,
            initialValue = initialValues[IdentifierSpec.Email]
        )
    )

    companion object {
        val DEFAULT_API_PATH = IdentifierSpec.Email
    }
}
