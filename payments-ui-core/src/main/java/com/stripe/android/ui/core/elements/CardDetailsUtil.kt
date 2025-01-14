package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.convertTo4DigitDate
import com.stripe.android.uicore.forms.FormFieldEntry

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CardDetailsUtil {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun createExpiryDateFormFieldValues(entry: FormFieldEntry): Map<IdentifierSpec, FormFieldEntry> {
        return mapOf(
            IdentifierSpec.CardExpMonth to getExpiryMonthFormFieldEntry(entry),
            IdentifierSpec.CardExpYear to getExpiryYearFormFieldEntry(entry)
        )
    }

    internal fun getExpiryMonthFormFieldEntry(entry: FormFieldEntry): FormFieldEntry {
        var month = -1
        entry.value?.let { date ->
            val newString = convertTo4DigitDate(date)
            if (newString.length == 4) {
                month = requireNotNull(newString.take(2).toIntOrNull())
            }
        }

        return entry.copy(
            value = month.toString().padStart(length = 2, padChar = '0')
        )
    }

    internal fun getExpiryYearFormFieldEntry(entry: FormFieldEntry): FormFieldEntry {
        var year = -1
        entry.value?.let { date ->
            val newString = convertTo4DigitDate(date)
            if (newString.length == 4) {
                year = requireNotNull(newString.takeLast(2).toIntOrNull()) + 2000
            }
        }

        return entry.copy(
            value = year.toString()
        )
    }
}