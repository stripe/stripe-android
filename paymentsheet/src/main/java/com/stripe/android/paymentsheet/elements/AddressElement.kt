package com.stripe.android.paymentsheet.elements

import androidx.annotation.VisibleForTesting
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.forms.FormFieldEntry
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

internal class AddressElement constructor(
    _identifier: IdentifierSpec,
    private val addressFieldRepository: AddressFieldElementRepository,
    private var args: FormFragmentArguments? = null,
    countryCodes: Set<String> = emptySet(),
    countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        args?.billingDetails?.address?.country
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
            args?.let {
                fields.forEach { field ->
                    field.setRawValue(it)
                }
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
                .associate { sectionFieldElement ->
                    sectionFieldElement.identifier to
                        sectionFieldElement.controller
                }
                .map {
                    getCurrentFieldValuePair(it.key, it.value)
                }
        ) {
            it.toList()
        }
    }

    private fun getCurrentFieldValuePair(
        identifier: IdentifierSpec,
        controller: InputController
    ) = combine(
        controller.rawFieldValue,
        controller.isComplete
    ) { rawFieldValue, isComplete ->
        Pair(
            identifier,
            FormFieldEntry(
                value = rawFieldValue,
                isComplete = isComplete,
            )
        )
    }
}
