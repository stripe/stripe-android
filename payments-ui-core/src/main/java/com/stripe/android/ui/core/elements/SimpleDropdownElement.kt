package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SectionSingleFieldElement

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SimpleDropdownElement(
    override val identifier: FormFieldId,
    override val controller: DropdownFieldController
) : SectionSingleFieldElement(identifier) {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null
}
