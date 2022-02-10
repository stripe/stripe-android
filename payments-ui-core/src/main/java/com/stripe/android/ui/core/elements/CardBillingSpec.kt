package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.parcelize.Parcelize

@Parcelize
internal object CardBillingSpec : SectionFieldSpec(IdentifierSpec.Generic("card_billing")) {
    fun transform(
        addressRepository: AddressFieldElementRepository
    ) = CardBillingAddressElement(
        IdentifierSpec.Generic("credit_billing"),
        addressRepository
    )
}
