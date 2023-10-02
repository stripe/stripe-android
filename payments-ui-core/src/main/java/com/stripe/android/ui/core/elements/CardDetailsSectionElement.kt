package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardDetailsSectionElement(
    val context: Context,
    initialValues: Map<IdentifierSpec, String?>,
    private val collectName: Boolean = false,
    private val isEligibleForCardBrandChoice: Boolean = false,
    override val identifier: IdentifierSpec,
    override val controller: CardDetailsSectionController = CardDetailsSectionController(
        context = context,
        initialValues = initialValues,
        collectName = collectName,
        isEligibleForCardBrandChoice = isEligibleForCardBrandChoice,
    ),
) : FormElement {
    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        controller.cardDetailsElement.getFormFieldValueFlow()

    override fun getTextFieldIdentifiers(): Flow<List<IdentifierSpec>> =
        controller.cardDetailsElement.getTextFieldIdentifiers()
}
