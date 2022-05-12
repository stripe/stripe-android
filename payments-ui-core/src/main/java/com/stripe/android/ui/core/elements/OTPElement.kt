package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class OTPElement(
    override val identifier: IdentifierSpec,
    override val controller: OTPController,
) : FormElement() {
    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return controller.fieldValue.map {
            listOf(identifier to FormFieldEntry(it, it.length == controller.otpLength))
        }
    }
}
