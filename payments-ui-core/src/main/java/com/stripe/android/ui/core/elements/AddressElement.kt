package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.elements.autocomplete.DefaultIsPlacesAvailable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class AddressElement constructor(
    _identifier: IdentifierSpec,
    private val addressRepository: AddressRepository,
    private var rawValuesMap: Map<IdentifierSpec, String?> = emptyMap(),
    private val addressType: AddressType = AddressType.Normal(),
    countryCodes: Set<String> = emptySet(),
    countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        rawValuesMap[IdentifierSpec.Country]
    ),
    sameAsShippingElement: SameAsShippingElement?,
    shippingValuesMap: Map<IdentifierSpec, String?>?
) : SectionMultiFieldElement(_identifier) {

    @VisibleForTesting
    val countryElement = CountryElement(
        IdentifierSpec.Country,
        countryDropdownFieldController
    )

    private val nameElement = SimpleTextElement(
        IdentifierSpec.Name,
        SimpleTextFieldController(
            textFieldConfig = SimpleTextFieldConfig(
                label = R.string.address_label_name
            ),
            initialValue = rawValuesMap[IdentifierSpec.Name]
        )
    )

    private val addressAutoCompleteElement = AddressTextFieldElement(
        identifier = IdentifierSpec.OneLineAddress,
        config = SimpleTextFieldConfig(label = R.string.address_label_address),
        onNavigation = (addressType as? AddressType.ShippingCondensed)?.onNavigation
    )

    private val phoneNumberElement = PhoneNumberElement(
        IdentifierSpec.Phone,
        PhoneNumberController(
            initialPhoneNumber = rawValuesMap[IdentifierSpec.Phone] ?: "",
            showOptionalLabel = addressType.phoneNumberState == PhoneNumberState.OPTIONAL
        )
    )

    private val currentValuesMap = mutableMapOf<IdentifierSpec, String?>()

    private val otherFields = countryElement.controller.rawFieldValue
        .distinctUntilChanged()
        .map { countryCode ->
            countryCode?.let {
                phoneNumberElement.controller.countryDropdownController.onRawValueChange(it)
            }
            addressRepository.get(countryCode)
                ?: emptyList()
        }
        .onEach { fields ->
            fields.forEach { field ->
                field.setRawValue(rawValuesMap)
            }
        }

    val fields = combine(
        countryElement.controller.rawFieldValue,
        otherFields,
        otherFields.flatMapLatest { fieldElements ->
            combine(
                fieldElements
                    .map {
                        it.getFormFieldValueFlow()
                    }
            ) {
                it.toList().flatten()
            }
        }.distinctUntilChanged(),
        sameAsShippingElement?.controller?.value ?: flowOf(null)
    ) { country, otherFields, otherFieldsValues, sameAsShipping ->
        val condensed = listOf(nameElement, countryElement, addressAutoCompleteElement)
        val expanded = listOf(nameElement, countryElement).plus(otherFields)
        val baseElements = when (addressType) {
            is AddressType.ShippingCondensed -> {
                // If the merchant has supplied Google Places API key, Google Places SDK is
                // available, and country is supported, use autocomplete
                val autocompleteSupportsCountry = autocompleteSupportedCountries.contains(country)
                val autocompleteAvailable = DefaultIsPlacesAvailable().invoke() &&
                    !addressType.googleApiKey.isNullOrBlank()
                if (autocompleteSupportsCountry && autocompleteAvailable) {
                    condensed
                } else {
                    expanded
                }
            }
            is AddressType.ShippingExpanded -> {
                expanded
            }
            else -> {
                listOf(countryElement).plus(otherFields)
            }
        }

        val fields = if (addressType.phoneNumberState != PhoneNumberState.HIDDEN) {
            baseElements.plus(phoneNumberElement)
        } else {
            baseElements
        }

        country?.let {
            currentValuesMap[IdentifierSpec.Country] = it
        }

        currentValuesMap.putAll(
            otherFieldsValues.associate {
                Pair(it.first, it.second.value)
            }
        )

//        println("James: ${otherFieldsValues.map { "${it.second.value}" }}")
//        println("James: ${currentValuesMap.map { "${it.value}" }}")
//        println("James: ${shippingValuesMap?.map { it.value ?: "" }}")



//        if (!same) {
//            sameAsShippingElement?.setRawValue(mapOf(sameAsShippingElement.identifier to "false"))
//        }

        // if current values == shipping values, then set same as shipping to true
        // if current values != shipping values, then set same as shipping to false
        // if sameAsShipping = true, set values to shippingValues map
        // if sameAsShipping = false, set values to original map

        sameAsShippingElement?.let {
            when (sameAsShipping) {
                true -> {
                    fields.forEach { field ->
                        val values = shippingValuesMap ?: emptyMap()
                        field.setRawValue(values)
                    }
                }
                false -> {
                    val values = currentValuesMap.mapValues {
                        if (it.key == IdentifierSpec.Country) it.value
                        else rawValuesMap[it.key] ?: ""
                    }
                    fields.forEach { field ->
                        field.setRawValue(values)
                    }
                }
                else -> {}
            }
        }

        val same = currentValuesMap.all {
            (shippingValuesMap?.get(it.key) ?: "") == it.value
        }
//        sameAsShippingElement?.setRawValue(mapOf(sameAsShippingElement.identifier to same.toString()))

        fields
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

    companion object {
        private val autocompleteSupportedCountries = setOf(
            "AU", "BE", "BR", "CA", "CH", "DE", "ES", "FR", "GB", "IE", "IN", "IT", "JP", "MX", "MY",
            "NO", "NL", "PH", "PL", "RU", "SE", "SG", "TR", "US", "ZA"
        )
    }
}
