package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import kotlinx.coroutines.flow.combine
import java.util.Calendar

internal class CardDetailsElement(
    identifier: IdentifierSpec,
    val controller: CardController = CardController(),
) : SectionMultiFieldElement(identifier) {

    /**
     * This will return a controller that abides by the SectionFieldErrorController interface.
     */
    override fun sectionFieldErrorController(): SectionFieldErrorController =
        controller

    override fun setRawValue(formFragmentArguments: FormFragmentArguments) {
        TODO("Not yet implemented")
    }

    override fun getFormFieldValueFlow() = combine(
        controller.numberElement.controller.formFieldValue,
        controller.cvcElement.controller.formFieldValue,
        controller.expirationDateElement.controller.formFieldValue
    ) { number, cvc, expirationDate ->
        var month = -1
        var year = -1
        expirationDate.value?.let { date ->
            // TODO: Duplicate of DateConfig
            val newString =
                if ((
                        date.isNotBlank() &&
                            !(date[0] == '0' || date[0] == '1')
                        ) ||
                    (
                        (date.length > 1) &&
                            (date[0] == '1' && requireNotNull(date[1].digitToInt()) > 2)
                        )
                ) {
                    "0$date"
                } else {
                    date
                }
            if (newString.length == 4) {
                month = requireNotNull(newString.take(2).toIntOrNull())
                year = requireNotNull(newString.takeLast(2).toIntOrNull())
                val yearMinus1900 = year + (2000 - 1900)
                val currentYear = Calendar.getInstance().get(Calendar.YEAR) - 1900
                val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
                if ((yearMinus1900 - currentYear) < 0) {
                    TextFieldStateConstants.Error.Invalid(R.string.invalid_expiry_year_past)
                } else if ((yearMinus1900 - currentYear) > 50) {
                    TextFieldStateConstants.Error.Invalid(R.string.invalid_expiry_year)
                } else if ((yearMinus1900 - currentYear) == 0 && currentMonth > month) {
                    TextFieldStateConstants.Error.Invalid(R.string.invalid_expiry_year_past)
                } else if (month !in 1..12) {
                    TextFieldStateConstants.Error.Incomplete(R.string.invalid_expiry_month)
                } else {
                    TextFieldStateConstants.Valid.Full
                }
            }
        }


        listOf(
            controller.numberElement.identifier to number,
            controller.cvcElement.identifier to cvc,
            IdentifierSpec.Generic("exp_month") to expirationDate.copy(
                value = month.toString()
            ),
            IdentifierSpec.Generic("exp_year") to expirationDate.copy(
                value = year.toString()
            )
        )
    }
}
