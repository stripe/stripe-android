package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
data class EmailSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Email
) : FormItemSpec() {
    fun transform(initialValues: Map<IdentifierSpec, String?>) = createSectionElement(
        EmailElement(
            this.apiPath,
            initialValue = initialValues[IdentifierSpec.Email]
        )
    )
}
