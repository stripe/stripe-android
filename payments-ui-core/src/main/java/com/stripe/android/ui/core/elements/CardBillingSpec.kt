package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
data class CardBillingSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = DEFAULT_API_PATH,
    @SerialName("allowed_country_codes")
    val allowedCountryCodes: Set<String> = DEFAULT_ALLOWED_COUNTRIES
) : FormItemSpec() {

    fun transform(
        addressRepository: AddressFieldElementRepository,
        initialValues: Map<IdentifierSpec, String?>
    ) = createSectionElement(
        CardBillingAddressElement(
            IdentifierSpec.Generic("credit_billing"),
            addressFieldRepository = addressRepository,
            countryCodes = allowedCountryCodes,
            rawValuesMap = initialValues
        ),
        label = R.string.billing_details
    )

    companion object {
        val DEFAULT_API_PATH = IdentifierSpec.Generic("card_billing")
        val DEFAULT_ALLOWED_COUNTRIES = supportedBillingCountries
    }
}
