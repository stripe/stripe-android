package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is the specification for a country field.
 * @property onlyShowCountryCodes: a list of country code that should be shown.  If empty all
 * countries will be shown.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@SerialName("country")
data class CountrySpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Country,
    val onlyShowCountryCodes: Set<String> = emptySet()
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>
    ) = createSectionElement(
        CountryElement(
            this.api_path,
            DropdownFieldController(
                CountryConfig(this.onlyShowCountryCodes),
                initialValue = initialValues[this.api_path]
            )
        )
    )
}
