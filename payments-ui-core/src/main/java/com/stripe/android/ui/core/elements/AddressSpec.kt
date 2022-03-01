package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class AddressSpec(
    override val identifier: IdentifierSpec,
) : SectionFieldSpec(identifier) {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>,
        addressRepository: AddressFieldElementRepository
    ): SectionFieldElement =
        AddressElement(
            IdentifierSpec.Generic("billing"),
            addressRepository,
            initialValues
        )
}
