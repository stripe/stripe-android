package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressTextFieldElement(
    override val identifier: IdentifierSpec,
    label: ResolvableString,
    onNavigation: (() -> Unit)? = null,
    inlinePredictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState>? = null,
    onInlinePredictionSelected: ((String) -> Unit)? = null,
    onInlineDismissed: (() -> Unit)? = null,
    getAttributionDrawable: ((Boolean) -> Int?)? = null,
) : SectionSingleFieldElement(identifier) {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override val controller: AddressTextFieldController =
        AddressTextFieldController(
            label = label,
            onNavigation = onNavigation,
            inlinePredictionsState = inlinePredictionsState,
            onInlinePredictionSelected = onInlinePredictionSelected,
            onInlineDismissed = onInlineDismissed,
            getAttributionDrawable = getAttributionDrawable,
        )

    val inlineQuery: StateFlow<String> get() = controller.inlineQuery

    override fun getTextFieldIdentifiers(): StateFlow<List<IdentifierSpec>> {
        return MutableStateFlow(listOf(identifier))
    }
}
