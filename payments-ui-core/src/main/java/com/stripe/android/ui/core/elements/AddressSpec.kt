package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class DisplayField {
    @SerialName("country")
    Country
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed class AddressType {
    abstract val phoneNumberState: PhoneNumberState

    data class ShippingCondensed(
        val googleApiKey: String?,
        override val phoneNumberState: PhoneNumberState,
        val onNavigation: () -> Unit
    ) : AddressType()

    data class ShippingExpanded(
        override val phoneNumberState: PhoneNumberState
    ) : AddressType()

    data class Normal(
        override val phoneNumberState: PhoneNumberState =
            PhoneNumberState.HIDDEN
    ) : AddressType()
}

@Serializable
enum class PhoneNumberState {
    @SerialName("hidden")
    HIDDEN,

    @SerialName("optional")
    OPTIONAL,

    @SerialName("required")
    REQUIRED
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
data class AddressSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("billing_details[address]"),

    @SerialName("allowed_country_codes")
    val allowedCountryCodes: Set<String> = supportedBillingCountries,

    @SerialName("display_fields")
    val displayFields: Set<DisplayField> = emptySet(),

    @SerialName("show_label")
    val showLabel: Boolean = true,

    /**
     * This field is not deserialized, this field is used for the Address Element
     */
    @Transient
    val type: AddressType = AddressType.Normal()
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
                countryCodes = allowedCountryCodes,
                addressType = type
            )
        },
        label = if (showLabel) R.string.billing_details else null
    )
}
