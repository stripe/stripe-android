package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DisplayField {
    @SerialName("country")
    Country
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
data class AddressSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = DEFAULT_API_PATH,

    @SerialName("allowed_country_codes")
    val allowedCountryCodes: Set<String> = DEFAULT_ALLOWED_COUNTRY_CODES,

    @SerialName("display_fields")
    val displayFields: Set<DisplayField> = emptySet()
) : FormItemSpec() {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>,
        addressRepository: AddressFieldElementRepository
    ) = createSectionElement(
        if (displayFields.size == 1 && displayFields.first() == DisplayField.Country) {
            CountryElement(
                IdentifierSpec.Generic("billing_details[address][country]"),
                DropdownFieldController(
                    CountryConfig(this.allowedCountryCodes),
                    initialValue = initialValues[this.apiPath]
                )
            )
        } else {
            AddressElement(
                apiPath,
                addressRepository,
                initialValues,
                countryCodes = allowedCountryCodes
            )
        },
        label = R.string.billing_details
    )

    companion object {
        val DEFAULT_API_PATH = IdentifierSpec.Generic("billing_details[address]")
        val DEFAULT_ALLOWED_COUNTRY_CODES = supportedBillingCountries
    }
}
