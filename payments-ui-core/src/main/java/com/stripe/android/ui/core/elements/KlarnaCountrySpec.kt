package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * This is the specification for a klarna country field
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
@Parcelize
class KlarnaCountrySpec(
    override val apiPath: IdentifierSpec = IdentifierSpec.Country
) : FormItemSpec(), RequiredItemSpec {
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
}
