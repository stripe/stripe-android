package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class PhoneSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Phone
) : FormItemSpec() {
    fun transform(initialValues: Map<IdentifierSpec, String?>) = createSectionElement(
        PhoneNumberElement(
            this.apiPath,
            PhoneNumberController(
                initialPhoneNumber = initialValues[IdentifierSpec.Phone] ?: "",
            )
        )
    )
}
