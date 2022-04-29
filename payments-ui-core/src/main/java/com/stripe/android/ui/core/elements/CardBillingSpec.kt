package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Parcelize
data class CardBillingSpec(
    override val identifier: IdentifierSpec = IdentifierSpec.Generic("card_billing"),
    val countryCodes: Set<String>
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        addressRepository: AddressFieldElementRepository,
        initialValues: Map<IdentifierSpec, String?>
    ) = createSectionElement(
        CardBillingAddressElement(
            IdentifierSpec.Generic("credit_billing"),
            addressFieldRepository = addressRepository,
            countryCodes = countryCodes,
            rawValuesMap = initialValues
        ),
        label = R.string.billing_details
    )
}
