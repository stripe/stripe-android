package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.convertTo4DigitDate
import com.stripe.android.uicore.forms.FormFieldEntry

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CardDetailsUtil {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun createExpiryDateFormFieldValues(entry: FormFieldEntry): Map<FormFieldId, FormFieldEntry> {
        return mapOf(
            FormFieldId.CardExpMonth to getExpiryMonthFormFieldEntry(entry),
            FormFieldId.CardExpYear to getExpiryYearFormFieldEntry(entry)
        )
    }

    @SuppressWarnings("MagicNumber")
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

    @SuppressWarnings("MagicNumber")
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
