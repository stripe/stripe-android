package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class OTPElement(
    override val identifier: IdentifierSpec,
    override val controller: OTPController
) : FormElement {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return controller.fieldValue.mapAsStateFlow {
            listOf(identifier to FormFieldEntry(it, it.length == controller.otpLength))
        }
    }

    /**
     * A [Flow] that emits the OTP value entered by the user.
     */
    val otpCompleteFlow: Flow<String>
        get() = getFormFieldValueFlow().mapNotNull { formFieldsList ->
            // formFieldsList contains only one element, for the OTP. Take the second value of
            // the pair, which is the FormFieldEntry containing the value entered by the user.
            formFieldsList.firstOrNull()?.second?.takeIf { it.isComplete }?.value
        }
}
