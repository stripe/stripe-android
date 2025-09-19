package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressTextFieldElement(
    override val identifier: IdentifierSpec,
    label: ResolvableString,
    onNavigation: (() -> Unit)? = null
) : SectionSingleFieldElement(identifier) {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override val controller: AddressTextFieldController =
        AddressTextFieldController(
            label = label,
            onNavigation = onNavigation
        )

    override fun getTextFieldIdentifiers(): StateFlow<List<IdentifierSpec>> {
        return MutableStateFlow(listOf(identifier))
    }
}
