package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import kotlinx.coroutines.flow.combine

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RowController(
    val fields: List<SectionSingleFieldElement>
) : SectionFieldErrorController, SectionFieldComposable {

    override val error = combine(
        fields.map { it.sectionFieldErrorController().error }
    ) {
        it.filterNotNull().firstOrNull()
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?,
        nextFocusDirection: FocusDirection,
        previousFocusDirection: FocusDirection
    ) {
        RowElementUI(
            enabled,
            this,
            hiddenIdentifiers,
            lastTextFieldIdentifier
        )
    }
}
