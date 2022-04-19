package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Parcelize
data class CardBillingSpec(
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
