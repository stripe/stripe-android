package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * This is an element that will make elements (as specified by identifier) hidden
 * when "save for future" use is unchecked
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SaveForFutureUseElement(
    val initialValue: Boolean,
    val merchantName: String?
) : FormElement {
    override val controller: SaveForFutureUseController = SaveForFutureUseController(
        initialValue
    )

    override val identifier: IdentifierSpec = IdentifierSpec.SaveForFutureUse

    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        controller.formFieldValue.map {
            listOf(
                identifier to it
            )
        }
}
