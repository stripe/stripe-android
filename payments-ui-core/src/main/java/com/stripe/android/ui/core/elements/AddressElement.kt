package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.ui.core.R
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
    private val addressType: AddressType = AddressType.Normal,
    countryCodes: Set<String> = emptySet(),
    countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        rawValuesMap[IdentifierSpec.Country]
    )
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
                label = R.string.address_label_name,
            ),
            initialValue = rawValuesMap[IdentifierSpec.Name]
        )
    )

    // TODO make this launch autocomplete.
    private val addressAutoCompleteElement = SimpleTextElement(
        IdentifierSpec.OneLineAddress,
        SimpleTextFieldController(
            textFieldConfig = SimpleTextFieldConfig(
                label = R.string.address_label_name,
            ),
            initialValue = rawValuesMap[IdentifierSpec.Name]
        )
    )

    private val phoneNumberElement = PhoneNumberElement(
        IdentifierSpec.Phone,
        PhoneNumberController(rawValuesMap[IdentifierSpec.Phone] ?: "")
    )

    private val otherFields = countryElement.controller.rawFieldValue
        .distinctUntilChanged()
        .map { countryCode ->
            countryCode?.let {
                phoneNumberElement.controller.countryDropdownController.onRawValueChange(it)
            }
            addressFieldRepository.get(countryCode)
                ?: emptyList()
        }
        .map { fields ->
            fields.forEach { field ->
                field.setRawValue(rawValuesMap)
            }
            fields
        }

    val fields = otherFields.map { otherFields ->
        val fieldsList = mutableListOf<SectionFieldElement>()
        if (addressType != AddressType.Normal) {
            fieldsList.addAll(listOf(nameElement))
        }
        fieldsList.add(countryElement)
        if (addressType != AddressType.ShippingCondensed) {
            fieldsList.addAll(otherFields)
        } else {
            fieldsList.add(addressAutoCompleteElement)
        }

        if (addressType != AddressType.Normal) {
            fieldsList.addAll(listOf(phoneNumberElement))
        }
        fieldsList
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
}
