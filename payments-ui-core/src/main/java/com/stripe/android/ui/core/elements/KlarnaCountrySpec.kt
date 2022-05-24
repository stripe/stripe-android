package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is the specification for a klarna country field
 */
@Serializable
@SerialName("klarna_country")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Parcelize
class KlarnaCountrySpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Country
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        currencyCode: String?,
        initialValues: Map<IdentifierSpec, String?>
    ) = createSectionElement(
        CountryElement(
            this.api_path,
            DropdownFieldController(
                CountryConfig(KlarnaHelper.getAllowedCountriesForCurrency(currencyCode)),
                initialValues[IdentifierSpec.Country]
            )
        )
    )
}
