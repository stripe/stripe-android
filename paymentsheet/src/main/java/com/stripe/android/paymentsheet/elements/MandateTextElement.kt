package com.stripe.android.paymentsheet.elements

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * This is an element that has static text because it takes no user input, it is not
 * outputted from the list of form field values.  If the stringResId contains a %s, the first
 * one will be populated in the form with the merchantName parameter.
 */
internal data class MandateTextElement(
    override val identifier: IdentifierSpec,
    val stringResId: Int,
    val color: Color,
    val merchantName: String?,
    override val controller: InputController? = null,
) : FormElement() {
    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        MutableStateFlow(emptyList())
}
