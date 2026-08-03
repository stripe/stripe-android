package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is the specification for a country field.
 * @property allowedCountryCodes: a list of country code that should be shown.  If empty all
 * countries will be shown.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
data class CountrySpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Country,
    @SerialName("allowed_country_codes")
    val allowedCountryCodes: Set<String> = CountryUtils.supportedBillingCountries
) : FormItemSpec() {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>
    ): SectionElement {
        return transform(
            initialValues = initialValues,
            shippingValues = null,
            autocompleteAddressInteractorFactory = null,
            addressCollectionMode = BillingAddressCollectionMode.Country(emptyMap()),
        ).single() as SectionElement
    }

    fun transform(
        initialValues: Map<IdentifierSpec, String?>,
        shippingValues: Map<IdentifierSpec, String?>?,
        autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
        addressCollectionMode: BillingAddressCollectionMode,
    ): List<FormElement> {
        val sameAsShippingElement = shippingValues
            ?.get(IdentifierSpec.SameAsShipping)
            ?.toBooleanStrictOrNull()
            ?.let { isSameAsShipping ->
                SameAsShippingElement(
                    identifier = IdentifierSpec.SameAsShipping,
                    controller = SameAsShippingController(isSameAsShipping),
                )
            }
            ?.takeIf { addressCollectionMode == BillingAddressCollectionMode.Full }
        val addressIdentifier = if (addressCollectionMode == BillingAddressCollectionMode.Full) {
            IdentifierSpec.BillingAddress
        } else {
            apiPath
        }
        val addressElement = BillingAddressElement(
            identifier = addressIdentifier,
            rawValuesMap = initialValues,
            countryCodes = allowedCountryCodes,
            countryDropdownFieldController = DropdownFieldController(
                CountryConfig(allowedCountryCodes),
                initialValue = initialValues[apiPath],
            ),
            countryElementIdentifier = apiPath,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = shippingValues,
            addressCollectionMode = addressCollectionMode,
            collectionConfiguration = BillingDetailsCollectionConfiguration(
                address = if (addressCollectionMode == BillingAddressCollectionMode.Full) {
                    BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
                } else {
                    BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
                },
                allowedCountries = allowedCountryCodes,
            ),
        )
        val label = if (addressCollectionMode == BillingAddressCollectionMode.Full) {
            R.string.stripe_billing_details.resolvableString
        } else {
            null
        }

        return listOfNotNull(
            createSectionElement(
                sectionFieldElement = addressElement,
                label = label,
            ),
            sameAsShippingElement,
        )
    }
}
