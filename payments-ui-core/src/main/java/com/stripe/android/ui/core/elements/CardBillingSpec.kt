package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
@SerialName("card_billing")
@Parcelize
data class CardBillingSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Generic("card_billing"),
    val valid_country_codes: Set<String> = supportedBillingCountries
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        addressRepository: AddressFieldElementRepository,
        initialValues: Map<IdentifierSpec, String?>
    ) = createSectionElement(
        CardBillingAddressElement(
            IdentifierSpec.Generic("credit_billing"),
            addressFieldRepository = addressRepository,
            countryCodes = valid_country_codes,
            rawValuesMap = initialValues
        ),
        label = R.string.billing_details
    )
}
