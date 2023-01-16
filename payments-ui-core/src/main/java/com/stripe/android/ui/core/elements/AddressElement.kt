package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.elements.autocomplete.DefaultIsPlacesAvailable
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SectionFieldErrorController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flattenConcat
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
                label = R.string.address_label_full_name
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

    /**
     * if [currentValuesMap] == [shippingValuesMap], then set sameAsShipping to true
     * if [currentValuesMap] != [shippingValuesMap], then set sameAsShipping to false
     * if sameAsShipping == true, set values to [shippingValuesMap] map
     * if sameAsShipping == false, set values to [currentValuesMap] map
     */
    private var lastSameAsShipping: Boolean? = null
    private val sameAsShippingUpdatedFlow = combine(
        otherFields,
        sameAsShippingElement?.controller?.value ?: flowOf(null)
    ) { fields, sameAsShippingValue ->
        val sameAsShipping = if (sameAsShippingValue != lastSameAsShipping) {
            lastSameAsShipping = sameAsShippingValue
            sameAsShippingValue
        } else {
            null
        }

        val allFields = listOf(countryElement).plus(fields)

        sameAsShipping?.let { same ->
            val values = if (same) {
                shippingValuesMap ?: emptyMap()
            } else {
                currentValuesMap.mapValues {
                    if (it.key == IdentifierSpec.Country) {
                        it.value
                    } else {
                        rawValuesMap[it.key] ?: ""
                    }
                }
            }
            allFields.forEach { field ->
                field.setRawValue(values)
            }
        }
    }

    @OptIn(FlowPreview::class)
    private val fieldsUpdatedFlow =
        combine(
            countryElement.controller.rawFieldValue,
            otherFields.map { fieldElements ->
                combine(
                    fieldElements
                        .map {
                            it.getFormFieldValueFlow()
                        }
                ) {
                    it.toList().flatten()
                }
            }.flattenConcat().distinctUntilChanged()
        ) { country, values ->
            country?.let {
                currentValuesMap[IdentifierSpec.Country] = it
            }
            currentValuesMap.putAll(
                values.associate {
                    Pair(it.first, it.second.value)
                }
            )
            val same = currentValuesMap.all {
                (shippingValuesMap?.get(it.key) ?: "") == it.value
            }
            lastSameAsShipping = same
            sameAsShippingElement?.setRawValue(
                mapOf(sameAsShippingElement.identifier to same.toString())
            )
        }

    val fields = combine(
        countryElement.controller.rawFieldValue,
        otherFields,
        sameAsShippingUpdatedFlow,
        fieldsUpdatedFlow
    ) { country, otherFields, _, _ ->
        val condensed = listOf(nameElement, countryElement, addressAutoCompleteElement)
        val expanded = listOf(nameElement, countryElement).plus(otherFields)
        val baseElements = when (addressType) {
            is AddressType.ShippingCondensed -> {
                // If the merchant has supplied Google Places API key, Google Places SDK is
                // available, and country is supported, use autocomplete
                val supportedCountries = addressType.autocompleteCountries
                val autocompleteSupportsCountry = supportedCountries
                    ?.map { it.toLowerCase(Locale.current) }
                    ?.contains(country?.toLowerCase(Locale.current)) == true
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

        fields
    }

    val controller = AddressController(fields)

    /**
     * This will return a controller that abides by the SectionFieldErrorController interface.
     */
    override fun sectionFieldErrorController(): SectionFieldErrorController =
        controller

    @OptIn(ExperimentalCoroutinesApi::class)
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
}
