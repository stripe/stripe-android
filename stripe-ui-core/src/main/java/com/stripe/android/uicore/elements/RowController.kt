package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.utils.combineAsStateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RowController(
    val fields: List<SectionSingleFieldElement>
) : SectionFieldErrorController, SectionFieldComposable {

    override val error = combineAsStateFlow(
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
        lastTextFieldIdentifier: IdentifierSpec?
    ) {
        RowElementUI(
            enabled,
            this,
            hiddenIdentifiers,
            lastTextFieldIdentifier,
            modifier = modifier,
        )
    }
}
