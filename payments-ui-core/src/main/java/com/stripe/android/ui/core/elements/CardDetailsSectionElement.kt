package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CardDetailsSectionElement(
    val context: Context,
    override val identifier: IdentifierSpec,
    override val controller: CardDetailsSectionController = CardDetailsSectionController(context),
) : FormElement() {
    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        controller.cardDetailsElement.getFormFieldValueFlow()

    override fun getTextFieldIdentifiers(): Flow<List<IdentifierSpec>> =
        controller.cardDetailsElement.getTextFieldIdentifiers()
}
