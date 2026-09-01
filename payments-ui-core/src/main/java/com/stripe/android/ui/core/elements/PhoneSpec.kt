package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElement
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class PhoneSpec(
    override val apiPath: IdentifierSpec = IdentifierSpec.Phone
) : FormItemSpec() {
    fun transform(initialValues: Map<IdentifierSpec, String?>) = createSectionElement(
        PhoneNumberElement(
            this.apiPath,
            PhoneNumberController.createPhoneNumberController(
                initialValue = initialValues[IdentifierSpec.Phone] ?: "",
            )
        )
    )
}
