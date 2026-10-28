package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.autofill.ContentType
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.R
import com.stripe.android.uicore.address.AddressSchemaRegistry
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.stripe.android.core.R as CoreR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressElement(
    _identifier: FormFieldId,
    private var rawValuesMap: Map<FormFieldId, String?> = emptyMap(),
    val addressInputMode: AddressInputMode = AddressInputMode.NoAutocomplete(),
    countryCodes: Set<String> = emptySet(),
    override val countryElement: CountryElement = CountryElement(
        FormFieldId.Country,
        DropdownFieldController(
            CountryConfig(countryCodes, autofillType = ContentType.AddressCountry),
            rawValuesMap[FormFieldId.Country]
        )
    ),
    sameAsShippingElement: SameAsShippingElement?,
    shippingValuesMap: Map<FormFieldId, String?>?,
    private val hideCountry: Boolean = false,
    private val inlineAutocompleteHandler: InlineAutocompleteHandler? = null,
) : SectionMultiFieldElement(_identifier), AddressFieldsElement {

    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    private val isValidating = MutableStateFlow(false)
    private val emailElement = EmailElement(
        initialValue = rawValuesMap[FormFieldId.Email]
    )

    private val nameElement = SimpleTextElement(
        FormFieldId.Name,
        SimpleTextFieldController(
            textFieldConfig = SimpleTextFieldConfig(
                label = resolvableString(CoreR.string.stripe_address_label_full_name),
                optional = addressInputMode.nameConfig == AddressFieldConfiguration.OPTIONAL,
                allowsEmojis = false,
            ),
            initialValue = rawValuesMap[FormFieldId.Name]
        )
    )

    private val addressAutoCompleteElement = AddressTextFieldElement(
        identifier = FormFieldId.OneLineAddress,
        label = resolvableString(R.string.stripe_address_label_address),
        inlineAutocompleteHandler = inlineAutocompleteHandler,
        reportsFormValue = false,
        initialQuery = "",
        showEnterManually = true,
    )

    private val expandedAutoCompleteElement: AddressTextFieldElement? =
        if (inlineAutocompleteHandler != null && addressInputMode is AddressInputMode.NoAutocomplete) {
            AddressTextFieldElement(
                identifier = FormFieldId.Line1,
                label = resolvableString(R.string.stripe_address_label_address),
                inlineAutocompleteHandler = inlineAutocompleteHandler,
                reportsFormValue = true,
                initialQuery = rawValuesMap[FormFieldId.Line1] ?: "",
                showEnterManually = false,
            )
        } else {
            null
        }

    val inlineQuery: StateFlow<String>
        get() = expandedAutoCompleteElement?.inlineQuery ?: addressAutoCompleteElement.inlineQuery

    @VisibleForTesting
    val phoneNumberElement = PhoneNumberElement(
        FormFieldId.Phone,
        PhoneNumberController.createPhoneNumberController(
            initialValue = rawValuesMap[FormFieldId.Phone] ?: "",
            showOptionalLabel = addressInputMode.phoneNumberConfig == AddressFieldConfiguration.OPTIONAL,
            acceptEmptyInput = addressInputMode.phoneNumberConfig == AddressFieldConfiguration.OPTIONAL,
        )
    )

    private val currentValuesMap = mutableMapOf<FormFieldId, String?>()

    private val elementsRegistry = AddressElementUiRegistry(AddressSchemaRegistry)

    private val otherFields = countryElement.controller.rawFieldValue
        .mapAsStateFlow { countryCode ->
            countryCode?.let {
                phoneNumberElement.controller.countryDropdownController.onRawValueChange(it)
            }
            (elementsRegistry.get(countryCode) ?: emptyList()).onEach { field ->
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
                    if (it.key == FormFieldId.Country) {
                        it.value
                    } else {
                        rawValuesMap[it.key] ?: ""
                    }
                }
            }
            allFields.forEach { field ->
                field.setRawValue(values)
            }
            syncExpandedLine1(values)
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
            },
            expandedAutoCompleteElement?.inlineQuery ?: stateFlowOf(""),
        ) { country, values, expandedLine1 ->
            country?.let {
                currentValuesMap[FormFieldId.Country] = it
            }
            currentValuesMap.putAll(
                values.associate {
                    Pair(it.first, it.second.value)
                }
            )
            if (expandedAutoCompleteElement != null) {
                currentValuesMap[FormFieldId.Line1] = expandedLine1
            }
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
        fieldsUpdatedFlow,
        isValidating,
    ) { _, otherFields, _, _, isValidating ->
        val hideName = addressInputMode.nameConfig == AddressFieldConfiguration.HIDDEN
        val headerElements = listOfNotNull(
            nameElement.takeUnless { hideName },
            countryElement.takeUnless { hideCountry },
        )

        val condensed = headerElements.plus(addressAutoCompleteElement)
        val baseElements = when (addressInputMode) {
            is AddressInputMode.AutocompleteInline -> condensed
            else -> {
                val bodyFields = otherFields.map { field ->
                    expandedAutoCompleteElement?.takeIf { field.identifier == FormFieldId.Line1 } ?: field
                }
                headerElements.plus(bodyFields)
            }
        }

        arrangeFieldsForInputMode(baseElements).apply {
            onEach {
                it.onValidationStateChanged(isValidating)
            }
        }
    }

    private val controller = AddressController(fields)

    override val addressController = stateFlowOf(controller)

    /**
     * This will return a controller that abides by the SectionFieldErrorController interface.
     */
    override fun sectionFieldErrorController(): SectionFieldValidationController =
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

    override fun getTextFieldIdentifiers(): StateFlow<List<FormFieldId>> = fields.flatMapLatestAsStateFlow {
        combineAsStateFlow(
            it
                .map {
                    it.getTextFieldIdentifiers()
                }
        ) {
            it.toList().flatten()
        }
    }

    override fun setRawValue(rawValuesMap: Map<FormFieldId, String?>) {
        this.rawValuesMap = rawValuesMap
        syncExpandedLine1(rawValuesMap)
    }

    private fun syncExpandedLine1(values: Map<FormFieldId, String?>) {
        expandedAutoCompleteElement?.controller?.onRawValueChange(values[FormFieldId.Line1] ?: "")
    }

    override fun onValidationStateChanged(isValidating: Boolean) {
        this.isValidating.value = isValidating
    }

    private fun arrangeFieldsForInputMode(
        baseElements: List<SectionFieldElement>,
    ): List<SectionFieldElement> {
        val emailField = when {
            addressInputMode.emailConfig != AddressFieldConfiguration.HIDDEN -> emailElement
            else -> null
        }
        val phoneField = when {
            addressInputMode.phoneNumberConfig != AddressFieldConfiguration.HIDDEN -> phoneNumberElement
            else -> null
        }
        val contactFieldsFirst = inlineAutocompleteHandler != null
        return if (contactFieldsFirst) {
            buildList {
                emailField?.let { add(it) }
                phoneField?.let { add(it) }
                addAll(baseElements)
            }
        } else {
            buildList {
                addAll(baseElements)
                emailField?.let { add(it) }
                phoneField?.let { add(it) }
            }
        }
    }
}
