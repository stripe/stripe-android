package com.stripe.android.paymentsheet

import android.util.Log
import com.stripe.android.paymentsheet.elements.AddressController
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class AddressSectionElement(
    override val identifier: IdentifierSpec,
    val addressSectionFieldRepository: AddressSectionFieldRepository
) : FormElement() {
    val focusRequesterCount = FocusRequesterCount()

    /**
     * Focus requester is a challenge - Must get this working from spec
     * other fields need to flow
     */
    val countryElement = SectionFieldElement.Country(
        IdentifierSpec("country"),
        DropdownFieldController(
            CountryConfig(
                setOf("US", "JP")
            )
        )
    )

    private val otherFields = countryElement.controller.rawFieldValue
        .distinctUntilChanged()
        .map { countryCode ->
            Log.e(
                "STRIPE", String.format(
                    "Country raw field updated. %s, %s, %s",
                    countryCode,
                    (addressSectionFieldRepository.get(countryCode)?.get(0)?.controller as? TextFieldController)?.isRequired,
                    addressSectionFieldRepository.get(countryCode)?.get(0)?.controller?.label,
                )
            )
            addressSectionFieldRepository.get(countryCode)
                ?: emptyList()
        }

    val fields = otherFields.map { listOf(countryElement).plus(it) }

    // Most section element controllers are created in the transform
    // instead of the element, where the label is created
    override val controller = AddressController(
        R.string.billing_details,
        fields
    )
}
