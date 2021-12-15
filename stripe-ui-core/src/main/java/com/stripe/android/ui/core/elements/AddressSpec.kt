package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AddressSpec(
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
