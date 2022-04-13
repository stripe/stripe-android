package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CardBillingSpec(
    override val identifier: IdentifierSpec = IdentifierSpec.Generic("card_billing"),
    val countryCodes: Set<String>
) : SectionFieldSpec(identifier) {
    fun transform(
        addressRepository: AddressFieldElementRepository
    ) = CardBillingAddressElement(
        IdentifierSpec.Generic("credit_billing"),
        addressRepository,
        countryCodes = countryCodes
    )
}
