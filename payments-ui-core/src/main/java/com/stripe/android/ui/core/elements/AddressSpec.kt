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
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("billing_details[address]"),

    @SerialName("valid_country_codes")
    val validCountryCodes: Set<String> = supportedBillingCountries,

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
                    CountryConfig(this.validCountryCodes),
                    initialValue = initialValues[this.apiPath]
                )
            )
        } else {
            AddressElement(
                apiPath,
                addressRepository,
                initialValues,
                countryCodes = validCountryCodes
            )
        },
        label = R.string.billing_details
    )
}
