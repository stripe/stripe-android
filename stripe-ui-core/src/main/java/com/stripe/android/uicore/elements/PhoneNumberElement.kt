package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class PhoneNumberElement(
    override val identifier: FormFieldId,
    override val controller: PhoneNumberController
) : SectionSingleFieldElement(identifier) {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<FormFieldId, FormFieldEntry>>> {
        return combineAsStateFlow(controller.formFieldValue, controller.phoneNumberFormatter) { phoneEntry, formatter ->
            listOf(
                identifier to phoneEntry,
                FormFieldId.PhoneNumberCountry to FormFieldEntry(
                    value = controller.getCountryCode(),
                    isComplete = true,
                ),
            )
        }
    }
}
