package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.SectionFieldErrorController
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * This is the element that represent the collection of all the card details:
 * card number, expiration date, and CVC.
 */
internal class CardDetailsElement(
    identifier: IdentifierSpec,
    context: Context,
    initialValues: Map<IdentifierSpec, String?>,
    viewOnlyFields: Set<IdentifierSpec> = emptySet(),
    val controller: CardDetailsController = CardDetailsController(
        context,
        initialValues,
        viewOnlyFields.contains(IdentifierSpec.CardNumber)
    )
) : SectionMultiFieldElement(identifier) {
    val isCardScanEnabled = controller.numberElement.controller.cardScanEnabled

    override fun sectionFieldErrorController(): SectionFieldErrorController =
        controller

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        // Nothing from FormArguments to populate
    }

    override fun getTextFieldIdentifiers(): Flow<List<IdentifierSpec>> =
        MutableStateFlow(
            listOf(
                controller.numberElement.identifier,
                controller.expirationDateElement.identifier,
                controller.cvcElement.identifier
            )
        )

    override fun getFormFieldValueFlow() = combine(
        controller.numberElement.controller.formFieldValue,
        controller.cvcElement.controller.formFieldValue,
        controller.expirationDateElement.controller.formFieldValue,
        controller.numberElement.controller.cardBrandFlow
    ) { number, cvc, expirationDate, brand ->
        listOf(
            controller.numberElement.identifier to number,
            controller.cvcElement.identifier to cvc,
            IdentifierSpec.CardBrand to FormFieldEntry(brand.code, true)
        ) + createExpiryDateFormFieldValues(expirationDate).toList()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun createExpiryDateFormFieldValues(
    entry: FormFieldEntry
): Map<IdentifierSpec, FormFieldEntry> {
    var month = -1
    var year = -1
    entry.value?.let { date ->
        val newString = convertTo4DigitDate(date)
        if (newString.length == 4) {
            month = requireNotNull(newString.take(2).toIntOrNull())
            year = requireNotNull(newString.takeLast(2).toIntOrNull()) + 2000
        }
    }

    val monthEntry = entry.copy(
        value = month.toString().padStart(length = 2, padChar = '0')
    )

    val yearEntry = entry.copy(
        value = year.toString()
    )

    return mapOf(
        IdentifierSpec.CardExpMonth to monthEntry,
        IdentifierSpec.CardExpYear to yearEntry
    )
}
