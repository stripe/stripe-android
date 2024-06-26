package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * This is an element that will make elements (as specified by identifier) hidden
 * when "save for future" use is unchecked
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SaveForFutureUseElement(
    val initialValue: Boolean,
    val merchantName: String?
) : FormElement {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override val controller: SaveForFutureUseController = SaveForFutureUseController(
        initialValue
    )

    override val identifier: IdentifierSpec = IdentifierSpec.SaveForFutureUse

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        controller.formFieldValue.mapAsStateFlow {
            listOf(
                identifier to it
            )
        }
}
