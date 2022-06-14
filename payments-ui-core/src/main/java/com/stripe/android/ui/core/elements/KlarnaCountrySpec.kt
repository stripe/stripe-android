package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable

/**
 * This is the specification for a klarna country field
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
class KlarnaCountrySpec(
    override val apiPath: IdentifierSpec = DEFAULT_API_PATH
) : FormItemSpec() {
    fun transform(
        currencyCode: String?,
        initialValues: Map<IdentifierSpec, String?>
    ) = createSectionElement(
        CountryElement(
            this.apiPath,
            DropdownFieldController(
                CountryConfig(KlarnaHelper.getAllowedCountriesForCurrency(currencyCode)),
                initialValues[IdentifierSpec.Country]
            )
        )
    )

    companion object {
        val DEFAULT_API_PATH = IdentifierSpec.Country
    }
}
