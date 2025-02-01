package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.R
import com.stripe.android.uicore.address.AddressSchemaRegistry
import com.stripe.android.uicore.address.AutocompleteCapableAddressType
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import com.stripe.android.core.R as CoreR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class AddressElement(
    _identifier: IdentifierSpec,
    private var rawValuesMap: Map<IdentifierSpec, String?> = emptyMap(),
    private val addressType: AddressType = AddressType.Normal(),
    countryCodes: Set<String> = emptySet(),
    countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        rawValuesMap[IdentifierSpec.Country]
    ),
    sameAsShippingElement: SameAsShippingElement?,
    shippingValuesMap: Map<IdentifierSpec, String?>?,
    private val isPlacesAvailable: IsPlacesAvailable = DefaultIsPlacesAvailable(),
    private val hideCountry: Boolean = false,
) : SectionMultiFieldElement(_identifier) {

    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val countryElement = CountryElement(
        IdentifierSpec.Country,
        countryDropdownFieldController
    )

    private val nameElement = SimpleTextElement(
        IdentifierSpec.Name,
        SimpleTextFieldController(
            textFieldConfig = SimpleTextFieldConfig(
                label = CoreR.string.stripe_address_label_full_name
            ),
            initialValue = rawValuesMap[IdentifierSpec.Name]
        )
    )

    private val addressAutoCompleteElement = AddressTextFieldElement(
        identifier = IdentifierSpec.OneLineAddress,
        config = SimpleTextFieldConfig(label = R.string.stripe_address_label_address),
        onNavigation = (addressType as? AddressType.ShippingCondensed)?.onNavigation
    )

    @VisibleForTesting
    val phoneNumberElement = PhoneNumberElement(
        IdentifierSpec.Phone,
        PhoneNumberController.createPhoneNumberController(
            initialValue = rawValuesMap[IdentifierSpec.Phone] ?: "",
            showOptionalLabel = addressType.phoneNumberState == PhoneNumberState.OPTIONAL,
            acceptAnyInput = addressType.phoneNumberState != PhoneNumberState.REQUIRED,
        )
    )

    private val currentValuesMap = mutableMapOf<IdentifierSpec, String?>()

    private val elementsRegistry = AddressElementUiRegistry(AddressSchemaRegistry)

    private val otherFields = countryElement.controller.rawFieldValue
        .mapAsStateFlow { countryCode ->
            countryCode?.let {
                phoneNumberElement.controller.countryDropdownController.onRawValueChange(it)
            }
            (elementsRegistry.get(countryCode) ?: emptyList()).onEach { field ->
                updateLine1WithAutocompleteAffordance(
                    field = field,
                    countryCode = countryCode,
                    addressType = addressType,
                    isPlacesAvailable = isPlacesAvailable,
                )
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
    private val sameAsShippingUpdatedFlow = combineAsStateFlow(
        otherFields,
        sameAsShippingElement?.controller?.value ?: stateFlowOf(null)
    ) { fields, sameAsShippingValue ->
        val sameAsShipping = if (sameAsShippingValue != lastSameAsShipping) {
            lastSameAsShipping = sameAsShippingValue
            sameAsShippingValue
        } else {
            null
        }

        val allFields = listOfNotNull(
            countryElement.takeUnless { hideCountry }
        ).plus(fields)

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

    private val fieldsUpdatedFlow =
        combineAsStateFlow(
            countryElement.controller.rawFieldValue,
            otherFields.flatMapLatestAsStateFlow { fieldElements ->
                combineAsStateFlow(
                    fieldElements
                        .map {
                            it.getFormFieldValueFlow()
                        }
                ) {
                    it.toList().flatten()
                }
            }
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

    val fields = combineAsStateFlow(
        countryElement.controller.rawFieldValue,
        otherFields,
        sameAsShippingUpdatedFlow,
        fieldsUpdatedFlow
    ) { country, otherFields, _, _ ->
        val condensed = listOfNotNull(
            nameElement,
            countryElement.takeUnless { hideCountry },
            addressAutoCompleteElement,
        )
        val expanded = listOfNotNull(
            nameElement,
            countryElement.takeUnless { hideCountry },
        ).plus(otherFields)
        val baseElements = when (addressType) {
            is AddressType.ShippingCondensed -> {
                // If the merchant has supplied Google Places API key, Google Places SDK is
                // available, and country is supported, use autocomplete
                if (addressType.supportsAutoComplete(country, isPlacesAvailable)) {
                    condensed
                } else {
                    expanded
                }
            }
            is AddressType.ShippingExpanded -> {
                expanded
            }
            else -> {
                listOfNotNull(
                    countryElement.takeUnless { hideCountry }
                ).plus(otherFields)
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

    override fun getFormFieldValueFlow() = fields.flatMapLatestAsStateFlow { fieldElements ->
        combineAsStateFlow(
            fieldElements
                .map {
                    it.getFormFieldValueFlow()
                }
        ) {
            it.toList().flatten()
        }
    }

    override fun getTextFieldIdentifiers(): StateFlow<List<IdentifierSpec>> = fields.flatMapLatestAsStateFlow {
        combineAsStateFlow(
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
        countryElement.setRawValue(rawValuesMap)
        elementsRegistry.get("US")?.forEach { element ->
            element.setRawValue(rawValuesMap)
        }
    }
}

internal fun updateLine1WithAutocompleteAffordance(
    field: SectionFieldElement,
    countryCode: String?,
    addressType: AddressType,
    isPlacesAvailable: IsPlacesAvailable,
) {
    if (field.identifier == IdentifierSpec.Line1) {
        val fieldController = (field as? SimpleTextElement)?.controller
        val config = (fieldController as? SimpleTextFieldController?)?.textFieldConfig
        val textConfig = config as? SimpleTextFieldConfig?
        if (textConfig != null) {
            updateLine1ConfigForAutocompleteAffordance(
                textConfig = textConfig,
                countryCode = countryCode,
                addressType = addressType,
                isPlacesAvailable = isPlacesAvailable,
            )
        }
    }
}

private fun updateLine1ConfigForAutocompleteAffordance(
    textConfig: SimpleTextFieldConfig,
    countryCode: String?,
    addressType: AddressType,
    isPlacesAvailable: IsPlacesAvailable,
) {
    val supportsAutocomplete = (addressType as? AutocompleteCapableAddressType)
        ?.supportsAutoComplete(countryCode, isPlacesAvailable)
    val icon: TextFieldIcon.Trailing? = if (supportsAutocomplete == true) {
        TextFieldIcon.Trailing(
            idRes = R.drawable.stripe_ic_search,
            isTintable = true,
            contentDescription = R.string.stripe_address_search_content_description,
            onClick = {
                addressType.onNavigation()
            }
        )
    } else {
        null
    }
    textConfig.trailingIcon.value = icon
}
