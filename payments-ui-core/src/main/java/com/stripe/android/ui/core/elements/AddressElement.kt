package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class AddressElement constructor(
    _identifier: IdentifierSpec,
    private val addressFieldRepository: AddressFieldElementRepository,
    private var rawValuesMap: Map<IdentifierSpec, String?> = emptyMap(),
    countryCodes: Set<String> = emptySet(),
    googlePlacesApiKey: String?,
    countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        rawValuesMap[IdentifierSpec.Country]
    ),
) : SectionMultiFieldElement(_identifier) {

    init {
        googlePlacesApiKey?.let {
            addressFieldRepository.initializeWithAutocomplete(googlePlacesApiKey)
        }
    }

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

    val fields = otherFields.map {
        listOf(countryElement).plus(it)
    }

    val controller = AddressController(fields)

    /**
     * This will return a controller that abides by the SectionFieldErrorController interface.
     */
    override fun sectionFieldErrorController(): SectionFieldErrorController =
        controller

    override fun getFormFieldValueFlow() = fields.flatMapLatest { fieldElements ->
        combine(
            fieldElements
                .map {
                    if (it is AutocompleteAddressTextFieldElement) {
                        it.controller.address.value?.let { address ->
                            setRawValue(
                                rawValuesMap.toMutableMap().apply {
                                    this[IdentifierSpec.Country] = address.country
                                    this[IdentifierSpec.Line1] = address.line1
                                    this[IdentifierSpec.City] = address.city
                                    this[IdentifierSpec.State] = address.state
                                    this[IdentifierSpec.PostalCode] = address.postalCode
                                }
                            )
                        }
                    }
                    it.getFormFieldValueFlow()
                }
        ) {
            it.toList().flatten()
        }
    }

    override fun getTextFieldIdentifiers(): Flow<List<IdentifierSpec>> = fields.flatMapLatest {
        combine(
            it
                .map {
                    it.getTextFieldIdentifiers()
                }
        ) {
            it.toList().flatten()
        }
    }

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        this.rawValuesMap = rawValuesMap
    }
}
