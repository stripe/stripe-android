package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AddressType
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class DisplayField {
    @SerialName("country")
    Country
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
data class AddressSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("billing_details[address]"),

    @SerialName("allowed_country_codes")
    val allowedCountryCodes: Set<String> = CountryUtils.supportedBillingCountries,

    @SerialName("display_fields")
    val displayFields: Set<DisplayField> = emptySet(),

    @SerialName("show_label")
    val showLabel: Boolean = true,

    /**
     * This field is not deserialized, this field is used for the Address Element
     */
    @Transient
    val type: AddressType = AddressType.Normal(),

    /**
     * This field is not deserialized, it is used for the special case where the address element
     * shouldn't show country.
     */
    @Transient
    val hideCountry: Boolean = false,
) : FormItemSpec() {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>,
        addressRepository: AddressRepository,
        shippingValues: Map<IdentifierSpec, String?>?,
    ): SectionElement? {
        val label = if (showLabel) R.string.stripe_billing_details else null
        return if (displayFields.size == 1 && displayFields.first() == DisplayField.Country) {
            createSectionElement(
                sectionFieldElement = CountryElement(
                    identifier = IdentifierSpec.Generic("billing_details[address][country]"),
                    controller = DropdownFieldController(
                        CountryConfig(this.allowedCountryCodes),
                        initialValue = initialValues[this.apiPath]
                    )
                ),
                label = label
            ).takeUnless { hideCountry }
        } else {
            val sameAsShippingElement =
                shippingValues?.get(IdentifierSpec.SameAsShipping)
                    ?.toBooleanStrictOrNull()
                    ?.let {
                        SameAsShippingElement(
                            identifier = IdentifierSpec.SameAsShipping,
                            controller = SameAsShippingController(it)
                        )
                    }
            val addressElement = AddressElement(
                _identifier = apiPath,
                addressRepository = addressRepository,
                rawValuesMap = initialValues,
                countryCodes = allowedCountryCodes,
                addressType = type,
                sameAsShippingElement = sameAsShippingElement,
                shippingValuesMap = shippingValues,
                hideCountry = hideCountry,
            )
            createSectionElement(
                sectionFieldElements = listOfNotNull(
                    addressElement,
                    sameAsShippingElement
                ),
                label = label
            )
        }
    }
}
