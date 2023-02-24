package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * This is an element that has static text because it takes no user input, it is not
 * outputted from the list of form field values.  If the stringResId contains a %s, the first
 * one will be populated in the form with the merchantName parameter.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AuBecsDebitMandateTextElement(
    override val identifier: IdentifierSpec,
    val merchantName: String?,
    override val controller: InputController? = null
) : FormElement {
    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        MutableStateFlow(emptyList())
}
