package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.elements.AddressController
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class AddressElement(
    override val identifier: IdentifierSpec,
    val addressFieldRepository: AddressFieldRepository,
    val countryCodes: Set<String> = setOf("US", "JP")
) : SectionFieldElement(),
    SectionFieldElementType.AddressElement {
    val focusRequesterCount = FocusRequesterCount()

    /**
     * Focus requester is a challenge - Must get this working from spec
     * other fields need to flow
     */
    val countryElement = Country(
        IdentifierSpec("country"),
        DropdownFieldController(CountryConfig(countryCodes))
    )

    private val otherFields = countryElement.controller.rawFieldValue
        .distinctUntilChanged()
        .map { countryCode ->
            addressFieldRepository.get(countryCode)
                ?: emptyList()
        }

    override val fields = otherFields.map { listOf(countryElement).plus(it) }

    // Most section element controllers are created in the transform
    // instead of the element, where the label is created
    override val controller = AddressController(fields)

    override fun controllerType(): SectionFieldElementType = this
}
