package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Suppress("EnumEntryName")
enum class DisplayField : Parcelable {
    country
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
@Parcelize
@SerialName("billing_address")
data class AddressSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Generic("billing_details[address]"),
    val valid_country_codes: Set<String> = supportedBillingCountries,
    val display_fields: Set<DisplayField> = emptySet()
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>,
        addressRepository: AddressFieldElementRepository
    ) = createSectionElement(
        if (display_fields.size == 1 && display_fields.first() == DisplayField.country) {
            CountryElement(
                IdentifierSpec.Generic("billing_details[address][country]"),
                DropdownFieldController(
                    CountryConfig(this.valid_country_codes),
                    initialValue = initialValues[this.api_path]
                )
            )
        } else {
            AddressElement(
                api_path,
                addressRepository,
                initialValues,
                countryCodes = valid_country_codes
            )
        },
        label = R.string.billing_details
    )
}
