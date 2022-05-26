package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
@Parcelize
data class CardBillingSpec(
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("card_billing"),
    @SerialName("valid_country_codes")
    val validCountryCodes: Set<String> = supportedBillingCountries
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        addressRepository: AddressFieldElementRepository,
        initialValues: Map<IdentifierSpec, String?>
    ) = createSectionElement(
        CardBillingAddressElement(
            IdentifierSpec.Generic("credit_billing"),
            addressFieldRepository = addressRepository,
            countryCodes = validCountryCodes,
            rawValuesMap = initialValues
        ),
        label = R.string.billing_details
    )
}
