package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
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
    @SerialName("allowed_country_codes")
    val allowedCountryCodes: Set<String> = CountryUtils.supportedBillingCountries
) : FormItemSpec() {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>,
        initialCountry: String? = null,
    ) = createSectionElement(
        BillingAddressElement(
            identifier = IdentifierSpec.Country,
            rawValuesMap = initialValues + listOfNotNull(
                initialCountry?.let { IdentifierSpec.Country to it },
            ).toMap(),
            countryCodes = allowedCountryCodes,
            countryDropdownFieldController = DropdownFieldController(
                CountryConfig(allowedCountryCodes),
                initialValue = initialCountry ?: initialValues[IdentifierSpec.Country],
            ),
            autocompleteAddressInteractorFactory = null,
            sameAsShippingElement = null,
            shippingValuesMap = null,
            addressCollectionMode = BillingAddressCollectionMode.Country(emptyMap()),
            collectionConfiguration = BillingDetailsCollectionConfiguration(
                address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                allowedCountries = allowedCountryCodes,
            ),
        )
    )
}
