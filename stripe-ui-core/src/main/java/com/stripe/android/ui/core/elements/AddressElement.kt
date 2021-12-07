package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressElement constructor(
    _identifier: IdentifierSpec,
    private val addressFieldRepository: AddressFieldElementRepository,
    private var rawValuesMap: Map<IdentifierSpec, String?> = emptyMap(),
    countryCodes: Set<String> = emptySet(),
    countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        rawValuesMap[IdentifierSpec.Country]
    ),
) : SectionMultiFieldElement(_identifier) {

    @VisibleForTesting
    val countryElement = CountryElement(
        IdentifierSpec.Country,
        countryDropdownFieldController
    )

    private val otherFields = countryElement.controller.rawFieldValue
        .distinctUntilChanged()
        .map { countryCode ->
            addressFieldRepository.get(countryCode)
                ?: emptyList()
        }
        .map { fields ->
            fields.forEach { field ->
                field.setRawValue(rawValuesMap)
            }
            fields
        }

    val fields = otherFields.map { listOf(countryElement).plus(it) }

    val controller = AddressController(fields)

    /**
     * This will return a controller that abides by the SectionFieldErrorController interface.
     */
    override fun sectionFieldErrorController(): SectionFieldErrorController =
        controller

    @ExperimentalCoroutinesApi
    override fun getFormFieldValueFlow() = fields.flatMapLatest { fieldElements ->
        combine(
            fieldElements
                .map {
                    it.getFormFieldValueFlow()
                }
        ) {
            it.toList().flatten()
        }
    }

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        this.rawValuesMap = rawValuesMap
    }
}
