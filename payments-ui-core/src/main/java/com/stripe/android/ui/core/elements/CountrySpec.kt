package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is the specification for a country field.
 * @property allowedCountryCodes: a list of country code that should be shown.  If empty all
 * countries will be shown.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class CountrySpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = DEFAULT_API_PATH,

    @SerialName("allowed_country_codes")
    val allowedCountryCodes: Set<String> = DEFAULT_ALLOWED_COUNTRY_CODES
) : FormItemSpec() {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>
    ) = createSectionElement(
        CountryElement(
            this.apiPath,
            DropdownFieldController(
                CountryConfig(this.allowedCountryCodes),
                initialValue = initialValues[this.apiPath]
            )
        )
    )

    companion object {
        val DEFAULT_API_PATH = IdentifierSpec.Country
        val DEFAULT_ALLOWED_COUNTRY_CODES = supportedBillingCountries
    }
}
